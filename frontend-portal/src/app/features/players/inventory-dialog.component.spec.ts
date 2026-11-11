import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';
import { NEVER, of, throwError } from 'rxjs';
import {
  CharacterInventory,
  EquipmentSlot,
  InventoryItem,
  InventoryService,
  ItemType,
  Rarity,
} from '../../core/api';
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

function anInventory(equipment: CharacterInventory['equipment'], bag: InventoryItem[]): CharacterInventory {
  return { characterId: 'c-1', coins: 1500, equipment, bag, setBonuses: [] };
}

describe('InventoryDialogComponent', () => {
  let inventoryService: jasmine.SpyObj<InventoryService>;
  let messageService: jasmine.SpyObj<MessageService>;
  let fixture: ComponentFixture<InventoryDialogComponent>;
  let dialog: InventoryDialogComponent;

  beforeEach(() => {
    inventoryService = jasmine.createSpyObj<InventoryService>('InventoryService', [
      'getInventory',
      'equip',
      'unequip',
      'move',
      'iconUrl',
    ]);
    inventoryService.iconUrl.and.callFake((path) => `http://api.test${path}`);
    messageService = jasmine.createSpyObj<MessageService>('MessageService', ['add']);
    TestBed.configureTestingModule({
      imports: [InventoryDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: InventoryService, useValue: inventoryService },
        { provide: MessageService, useValue: messageService },
      ],
    });
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

  /** Drags the item's tile onto the target, as a browser does it. */
  async function drag(itemId: string, target: string) {
    const dataTransfer = new DataTransfer();
    find(`[data-item="${itemId}"]`)!.dispatchEvent(new DragEvent('dragstart', { dataTransfer, bubbles: true }));
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

  it('takes drags again when it reopens while a change is on its way', async () => {
    await open(anInventory({}, [HELMET]));
    inventoryService.equip.and.returnValue(NEVER);
    await drag('helmet', '[data-slot="HEAD"]');

    dialog.close();
    await open(anInventory({}, [HELMET]));
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

  it('draws whole rows with a free one after the last item', () => {
    const cells = bagCells([{ ...HELMET, bagPosition: 3 * BAG_COLUMNS + 1 }]);

    expect(cells.length).toBe(5 * BAG_COLUMNS);
    expect(cells[3 * BAG_COLUMNS + 1]?.publicId).toBe('helmet');
  });
});
