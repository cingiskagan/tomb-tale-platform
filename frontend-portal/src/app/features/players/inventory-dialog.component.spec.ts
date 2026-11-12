import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Confirmation, ConfirmationService, MessageService } from 'primeng/api';
import { Subject, of, throwError } from 'rxjs';
import {
  CharacterInventory,
  DealerResponse,
  DealerService,
  EquipmentSlot,
  InventoryItem,
  InventoryService,
  ItemType,
  Rarity,
  StandEntry,
} from '../../core/api';
import { RUNTIME_CONFIG } from '../../core/config';
import { BAG_COLUMNS, InventoryDialogComponent, bagCells } from './inventory-dialog.component';

function anItem(publicId: string, overrides: Partial<InventoryItem>): InventoryItem {
  return {
    publicId,
    code: publicId.toUpperCase(),
    name: publicId,
    itemType: ItemType.RING,
    iconUrl: null,
    rarity: Rarity.GRAY,
    bonus: {},
    finalStats: {},
    quantity: 1,
    equipmentSlot: null,
    bagPosition: null,
    ...overrides,
  };
}

const RING = anItem('ring', { equipmentSlot: EquipmentSlot.RING_1, rarity: Rarity.BLUE, bonus: { FIRE_DAMAGE: 4 } });
const HELMET = anItem('helmet', { itemType: ItemType.HELMET, bagPosition: 2 });

function anInventory(
  equipment: CharacterInventory['equipment'],
  bag: InventoryItem[],
  coins = 1500,
): CharacterInventory {
  return { characterId: 'c-1', coins, equipment, bag, setBonuses: [] };
}

function aDealer(code: string, retired = false): DealerResponse {
  return { publicId: `id-${code}`, code, name: code, retired, version: 0, createdAt: '', updatedAt: '' };
}

const BLUE_SWORD: StandEntry = {
  publicId: 'e-1',
  code: 'SWORD_IRON',
  name: 'Iron Sword',
  itemType: ItemType.ONE_HANDED_WEAPON,
  iconUrl: null,
  baseStats: {},
  rarity: Rarity.BLUE,
  bonus: null,
  price: 1350,
};

describe('InventoryDialogComponent', () => {
  let inventoryService: jasmine.SpyObj<InventoryService>;
  let dealerService: jasmine.SpyObj<DealerService>;
  let messageService: jasmine.SpyObj<MessageService>;
  let confirmationService: ConfirmationService;
  let config: { issuerUri: string; clientId: string; apiBaseUrl: string; production: boolean };
  let fixture: ComponentFixture<InventoryDialogComponent>;
  let dialog: InventoryDialogComponent;

  beforeEach(() => {
    inventoryService = jasmine.createSpyObj<InventoryService>('InventoryService', [
      'getInventory',
      'equip',
      'unequip',
      'move',
      'buy',
      'sell',
      'iconUrl',
    ]);
    inventoryService.iconUrl.and.callFake((path) => `http://api.test${path}`);
    dealerService = jasmine.createSpyObj<DealerService>('DealerService', ['listDealers', 'getStand']);
    dealerService.listDealers.and.returnValue(
      of({
        content: [aDealer('TOWN_BLACKSMITH'), aDealer('TOWN_GONE', true)],
        page: { number: 0, size: 200, totalElements: 2, totalPages: 1 },
      }),
    );
    dealerService.getStand.and.returnValue(of({ code: 'TOWN_BLACKSMITH', name: 'Blacksmith', entries: [BLUE_SWORD] }));
    messageService = jasmine.createSpyObj<MessageService>('MessageService', ['add']);
    config = { issuerUri: 'http://issuer.test', clientId: 'portal', apiBaseUrl: 'http://api.test', production: false };
    TestBed.configureTestingModule({
      imports: [InventoryDialogComponent],
      providers: [
        provideNoopAnimations(),
        ConfirmationService,
        { provide: InventoryService, useValue: inventoryService },
        { provide: DealerService, useValue: dealerService },
        { provide: MessageService, useValue: messageService },
        { provide: RUNTIME_CONFIG, useValue: config },
      ],
    });
    confirmationService = TestBed.inject(ConfirmationService);
    fixture = TestBed.createComponent(InventoryDialogComponent);
    dialog = fixture.componentInstance;
  });

  async function open(inventory: CharacterInventory) {
    inventoryService.getInventory.and.returnValue(of(inventory));
    dialog.character = { publicId: 'c-1', name: 'Aria', level: 1, experiencePoints: 0, createdAt: '' };
    dialog.visible = true;
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function find(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector);
  }

  async function pickTheBlacksmith() {
    dialog.dealerCode.setValue('TOWN_BLACKSMITH');
    dialog.pickDealer();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  /** Drags the item's tile onto the target, as a browser does it. */
  async function drag(itemId: string, target: string) {
    await dragFrom(`[data-item="${itemId}"]`, target);
  }

  async function dragFrom(source: string, target: string) {
    const dataTransfer = new DataTransfer();
    find(source)!.dispatchEvent(new DragEvent('dragstart', { dataTransfer, bubbles: true }));
    find(target)!.dispatchEvent(new DragEvent('drop', { dataTransfer, bubbles: true, cancelable: true }));
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('loads the character it opens and shows the coins, the slots and the bag', async () => {
    await open(anInventory({ RING_1: RING }, [HELMET]));

    expect(inventoryService.getInventory).toHaveBeenCalledWith('c-1');
    expect(find('.coins')?.textContent).toContain('1,500');
    expect(find('[data-slot="RING_1"] [data-item="ring"]')?.classList).toContain('rarity-BLUE');
    expect(find('[data-cell="2"] [data-item="helmet"]')).not.toBeNull();
  });

  it('shows the bonus on hover', async () => {
    await open(anInventory({ RING_1: RING }, []));

    expect(find('[data-item="ring"]')?.title).toContain('Bonus: FIRE_DAMAGE 4');
  });

  it('equips a bag item dropped on a slot and redraws from the answer', async () => {
    await open(anInventory({}, [HELMET]));
    inventoryService.equip.and.returnValue(
      of(anInventory({ HEAD: { ...HELMET, equipmentSlot: EquipmentSlot.HEAD, bagPosition: null } }, [])),
    );

    await drag('helmet', '[data-slot="HEAD"]');

    expect(inventoryService.equip).toHaveBeenCalledWith('c-1', EquipmentSlot.HEAD, 'helmet');
    expect(find('[data-slot="HEAD"] [data-item="helmet"]')).not.toBeNull();
    expect(find('[data-cell="2"] [data-item="helmet"]')).toBeNull();
  });

  it('unequips an item dropped on the bag', async () => {
    await open(anInventory({ RING_1: RING }, []));
    inventoryService.unequip.and.returnValue(of(anInventory({}, [{ ...RING, equipmentSlot: null, bagPosition: 0 }])));

    await drag('ring', '[data-cell="5"]');

    expect(inventoryService.unequip).toHaveBeenCalledWith('c-1', EquipmentSlot.RING_1);
    expect(find('[data-cell="0"] [data-item="ring"]')).not.toBeNull();
  });

  it('moves a bag item to the cell it is dropped on', async () => {
    await open(anInventory({}, [HELMET]));
    inventoryService.move.and.returnValue(of(anInventory({}, [{ ...HELMET, bagPosition: 6 }])));

    await drag('helmet', '[data-cell="6"]');

    expect(inventoryService.move).toHaveBeenCalledWith('c-1', 6, 'helmet');
  });

  it("shows inventory's reason when it refuses a change", async () => {
    await open(anInventory({}, [HELMET]));
    inventoryService.equip.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { detail: 'RING_2 does not take HELMET' } })),
    );

    await drag('helmet', '[data-slot="RING_2"]');

    expect(messageService.add).toHaveBeenCalledWith(jasmine.objectContaining({ detail: 'RING_2 does not take HELMET' }));
    expect(find('[data-cell="2"] [data-item="helmet"]')).not.toBeNull();
  });

  it('reopened while a change is on its way, loads after the change and takes drags again', async () => {
    await open(anInventory({}, [HELMET]));
    const equipped = new Subject<CharacterInventory>();
    inventoryService.equip.and.returnValue(equipped);
    await drag('helmet', '[data-slot="HEAD"]');

    dialog.close();
    await open(anInventory({}, [HELMET]));
    expect(inventoryService.getInventory).toHaveBeenCalledTimes(1);
    expect(find('[data-item="helmet"]')).toBeNull();

    equipped.next(anInventory({}, [HELMET]));
    // The load waits on a chain of promises, which one macrotask lets finish.
    await new Promise((resolve) => setTimeout(resolve));
    fixture.detectChanges();
    expect(inventoryService.getInventory).toHaveBeenCalledTimes(2);

    inventoryService.move.and.returnValue(of(anInventory({}, [{ ...HELMET, bagPosition: 6 }])));
    await drag('helmet', '[data-cell="6"]');
    expect(inventoryService.move).toHaveBeenCalledWith('c-1', 6, 'helmet');
  });

  it('shows a generic icon for an icon that does not load', async () => {
    await open(anInventory({}, [{ ...HELMET, iconUrl: '/api/v1/inventory/icons/gone' }]));
    const tile = find('[data-item="helmet"]')!;
    expect(tile.querySelector('img')?.getAttribute('src')).toBe('http://api.test/api/v1/inventory/icons/gone');

    tile.querySelector('img')!.dispatchEvent(new Event('error'));
    fixture.detectChanges();

    expect(tile.querySelector('img')).toBeNull();
    expect(tile.querySelector('.generic-icon')).not.toBeNull();
  });

  it("offers the dealers that still sell, and shows the picked one's stand with its prices", async () => {
    await open(anInventory({}, []));
    expect(dialog.dealers.map((dealer) => dealer.code)).toEqual(['TOWN_BLACKSMITH']);
    expect(find('[data-entry]')).toBeNull();

    await pickTheBlacksmith();

    expect(dealerService.getStand).toHaveBeenCalledWith('TOWN_BLACKSMITH');
    expect(find('.stand-entry')?.textContent).toContain('1,350');
    expect(find('[data-entry="e-1"]')?.classList).toContain('rarity-BLUE');
  });

  it('buys a stand entry dropped on the bag, with a new trade id each time', async () => {
    await open(anInventory({}, []));
    await pickTheBlacksmith();
    const sword = anItem('sword', { itemType: ItemType.ONE_HANDED_WEAPON, rarity: Rarity.BLUE, bagPosition: 0 });
    inventoryService.buy.and.returnValue(of(anInventory({}, [sword], 150)));

    await dragFrom('[data-entry="e-1"]', '[data-cell="4"]');
    await dragFrom('[data-entry="e-1"]', '[data-cell="4"]');

    const [first, second] = inventoryService.buy.calls.allArgs();
    expect(first).toEqual(['c-1', jasmine.any(String), 'e-1', 1350]);
    expect(second[1]).not.toBe(first[1]);
    expect(find('[data-cell="0"] [data-item="sword"]')).not.toBeNull();
    expect(find('.coins')?.textContent).toContain('150');
  });

  it('sells a bag item dropped on the stand', async () => {
    await open(anInventory({}, [HELMET]));
    await pickTheBlacksmith();
    inventoryService.sell.and.returnValue(of(anInventory({}, [], 1525)));

    await drag('helmet', '.stand');

    expect(inventoryService.sell).toHaveBeenCalledWith('c-1', jasmine.any(String), 'helmet');
    expect(find('[data-item="helmet"]')).toBeNull();
    expect(find('.coins')?.textContent).toContain('1,525');
  });

  it("shows inventory's reason for a refused trade, and redraws the coins and the prices", async () => {
    await open(anInventory({}, []));
    await pickTheBlacksmith();
    inventoryService.buy.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'The price is now 1500 coins, not 1350' } })),
    );
    inventoryService.getInventory.and.returnValue(of(anInventory({}, [], 900)));
    dealerService.getStand.and.returnValue(
      of({ code: 'TOWN_BLACKSMITH', name: 'Blacksmith', entries: [{ ...BLUE_SWORD, price: 1500 }] }),
    );

    await dragFrom('[data-entry="e-1"]', '[data-cell="0"]');
    await new Promise((resolve) => setTimeout(resolve));
    fixture.detectChanges();

    expect(messageService.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ detail: 'The price is now 1500 coins, not 1350' }),
    );
    expect(find('.coins')?.textContent).toContain('900');
    expect(find('.stand-entry')?.textContent).toContain('1,500');
  });

  it("in production, asks before the admin's first change to a character, and only then", async () => {
    config.production = true;
    const confirm = spyOn(confirmationService, 'confirm');
    await open(anInventory({}, [HELMET]));
    inventoryService.move.and.returnValue(of(anInventory({}, [{ ...HELMET, bagPosition: 6 }])));

    confirm.and.callFake((confirmation: Confirmation) => {
      confirmation.reject?.();
      return confirmationService;
    });
    await drag('helmet', '[data-cell="6"]');
    expect(inventoryService.move).not.toHaveBeenCalled();

    confirm.and.callFake((confirmation: Confirmation) => {
      confirmation.accept?.();
      return confirmationService;
    });
    await drag('helmet', '[data-cell="6"]');
    await drag('helmet', '[data-cell="7"]');

    expect(confirm).toHaveBeenCalledTimes(2);
    expect(inventoryService.move).toHaveBeenCalledTimes(2);
  });

  it('never asks outside production', async () => {
    const confirm = spyOn(confirmationService, 'confirm');
    await open(anInventory({}, [HELMET]));
    inventoryService.move.and.returnValue(of(anInventory({}, [{ ...HELMET, bagPosition: 6 }])));

    await drag('helmet', '[data-cell="6"]');

    expect(confirm).not.toHaveBeenCalled();
    expect(inventoryService.move).toHaveBeenCalled();
  });

  it('draws whole rows with a free one after the last item', () => {
    const cells = bagCells([{ ...HELMET, bagPosition: 3 * BAG_COLUMNS + 1 }]);

    expect(cells.length).toBe(5 * BAG_COLUMNS);
    expect(cells[3 * BAG_COLUMNS + 1]?.publicId).toBe('helmet');
  });
});
