import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';
import {
  DealerService,
  InventoryService,
  ItemCatalogService,
  ItemTemplateResponse,
  ItemType,
  Rarity,
  StandEntry,
} from '../../core/api';
import { StandEditorComponent } from './stand-editor.component';

function aTemplate(code: string, retired = false): ItemTemplateResponse {
  return {
    publicId: `id-${code}`,
    code,
    name: code,
    itemType: ItemType.ONE_HANDED_WEAPON,
    maxStackSize: 1,
    price: 150,
    baseStats: {},
    assetKey: `Items/${code}`,
    iconHash: null,
    itemSetId: null,
    retired,
    version: 0,
    createdAt: '',
    updatedAt: '',
  };
}

function anEntry(publicId: string, rarity: Rarity, price: number, bonus: StandEntry['bonus'] = null): StandEntry {
  return {
    publicId,
    code: 'SWORD_IRON',
    name: 'Iron Sword',
    itemType: ItemType.ONE_HANDED_WEAPON,
    iconUrl: null,
    baseStats: {},
    rarity,
    bonus,
    price,
  };
}

const BLACKSMITH = {
  publicId: 'd-1',
  code: 'TOWN_BLACKSMITH',
  name: 'Blacksmith',
  retired: false,
  version: 0,
  createdAt: '',
  updatedAt: '',
};

describe('StandEditorComponent', () => {
  let dealerService: jasmine.SpyObj<DealerService>;
  let catalog: jasmine.SpyObj<ItemCatalogService>;
  let messageService: jasmine.SpyObj<MessageService>;
  let fixture: ComponentFixture<StandEditorComponent>;
  let editor: StandEditorComponent;

  beforeEach(() => {
    dealerService = jasmine.createSpyObj<DealerService>('DealerService', ['getStand', 'addEntry', 'removeEntry']);
    catalog = jasmine.createSpyObj<ItemCatalogService>('ItemCatalogService', ['listTemplates', 'iconUrl']);
    catalog.listTemplates.and.returnValue(
      of({
        content: [aTemplate('SWORD_IRON'), aTemplate('AXE_OLD', true)],
        page: { number: 0, size: 10, totalElements: 2, totalPages: 1 },
      }),
    );
    messageService = jasmine.createSpyObj<MessageService>('MessageService', ['add']);
    TestBed.configureTestingModule({
      imports: [StandEditorComponent],
      providers: [
        provideNoopAnimations(),
        { provide: DealerService, useValue: dealerService },
        { provide: ItemCatalogService, useValue: catalog },
        { provide: InventoryService, useValue: jasmine.createSpyObj<InventoryService>('InventoryService', ['iconUrl']) },
        { provide: MessageService, useValue: messageService },
      ],
    });
    fixture = TestBed.createComponent(StandEditorComponent);
    editor = fixture.componentInstance;
  });

  async function open(entries: StandEntry[], customEnabled = false) {
    dealerService.getStand.and.returnValue(of({ code: BLACKSMITH.code, name: BLACKSMITH.name, entries }));
    editor.customEnabled = customEnabled;
    editor.dealer = BLACKSMITH;
    editor.visible = true;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function find(selector: string): HTMLElement | null {
    return document.querySelector(selector);
  }

  /** Drags the template's catalog row onto the stand, as a browser does it. */
  async function dragOntoStand(code: string) {
    const dataTransfer = new DataTransfer();
    find(`[data-template="${code}"]`)!.dispatchEvent(new DragEvent('dragstart', { dataTransfer, bubbles: true }));
    find('section.stand')!.dispatchEvent(new DragEvent('drop', { dataTransfer, bubbles: true, cancelable: true }));
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => fixture.destroy());

  it("loads the stand by its dealer's code and shows each entry's price", async () => {
    await open([anEntry('e-1', Rarity.BLUE, 1350)]);

    expect(dealerService.getStand).toHaveBeenCalledWith('TOWN_BLACKSMITH');
    expect(find('[data-entry="e-1"]')?.textContent).toContain('1350');
  });

  it('adds a template dropped on the stand at the picked rarity', async () => {
    await open([]);
    editor.rarity.setValue(Rarity.PURPLE);
    dealerService.addEntry.and.returnValue(of(anEntry('e-2', Rarity.PURPLE, 4050)));

    await dragOntoStand('SWORD_IRON');

    expect(dealerService.addEntry).toHaveBeenCalledWith('TOWN_BLACKSMITH', {
      templateId: 'id-SWORD_IRON',
      rarity: Rarity.PURPLE,
    });
    expect(find('[data-entry="e-2"]')).not.toBeNull();
  });

  it('never adds a retired template', async () => {
    await open([]);

    await dragOntoStand('AXE_OLD');

    expect(dealerService.addEntry).not.toHaveBeenCalled();
  });

  it("shows inventory's reason when it refuses an entry", async () => {
    await open([]);
    dealerService.addEntry.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'The item template SWORD_IRON is retired' } })),
    );

    await dragOntoStand('SWORD_IRON');

    expect(messageService.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ detail: 'The item template SWORD_IRON is retired' }),
    );
  });

  it('takes an entry off the stand', async () => {
    await open([anEntry('e-1', Rarity.GRAY, 150)]);
    dealerService.removeEntry.and.returnValue(of(undefined));

    find('[data-entry="e-1"] button')!.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(dealerService.removeEntry).toHaveBeenCalledWith('TOWN_BLACKSMITH', 'e-1');
    expect(find('[data-entry="e-1"]')).toBeNull();
  });

  it('offers custom items only while inventory takes them', async () => {
    await open([anEntry('e-1', Rarity.RED, 36450, { FIRE_DAMAGE: 40 })]);
    expect(find('[aria-label="Custom item"]')).toBeNull();
    expect(find('[data-entry="e-1"]')?.textContent).toContain('Custom: FIRE_DAMAGE 40');
    fixture.destroy();

    fixture = TestBed.createComponent(StandEditorComponent);
    editor = fixture.componentInstance;
    await open([], true);
    expect(find('[data-template="SWORD_IRON"] [aria-label="Custom item"]')).not.toBeNull();
  });
});
