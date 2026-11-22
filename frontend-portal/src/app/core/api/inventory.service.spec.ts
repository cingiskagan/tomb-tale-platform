import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RUNTIME_CONFIG, RuntimeConfig } from '../config';
import { EquipmentSlot } from './inventory.models';
import { InventoryService } from './inventory.service';

const CONFIG: RuntimeConfig = {
  issuerUri: 'http://localhost:8080/realms/tombtale',
  clientId: 'test-client',
  apiBaseUrl: 'http://localhost:8080',
  production: false,
};

const CHARACTER = 'http://localhost:8080/api/v1/inventory/characters/c-1';

describe('InventoryService', () => {
  let service: InventoryService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: RUNTIME_CONFIG, useValue: CONFIG }],
    });
    service = TestBed.inject(InventoryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('equips the item the body names in the slot the path names', () => {
    service.equip('c-1', EquipmentSlot.HEAD, 'i-1').subscribe();

    const request = http.expectOne(`${CHARACTER}/equipment/HEAD`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ itemId: 'i-1' });
    request.flush({});
  });

  it('empties a slot with a delete that names the cell the item was dropped on', () => {
    service.unequip('c-1', EquipmentSlot.RING_2, 7).subscribe();

    expect(http.expectOne(`${CHARACTER}/equipment/RING_2?cell=7`).request.method).toBe('DELETE');
  });

  it('moves the item the body names to the cell the path names', () => {
    service.move('c-1', 7, 'i-1').subscribe();

    const request = http.expectOne(`${CHARACTER}/bag/7`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ itemId: 'i-1' });
  });

  it('buys with the trade id, the stand entry, the price the admin saw and the cell it was dropped on', () => {
    service.buy('c-1', 'trade-1', 'e-1', 1350, 4).subscribe();

    const request = http.expectOne(`${CHARACTER}/dealer-buys`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ tradeId: 'trade-1', entryId: 'e-1', price: 1350, cell: 4 });
  });

  it('sells a bag item with the trade id', () => {
    service.sell('c-1', 'trade-2', 'i-1').subscribe();

    const request = http.expectOne(`${CHARACTER}/dealer-sells`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ tradeId: 'trade-2', itemId: 'i-1' });
  });

  it('puts the API origin before an icon path', () => {
    expect(service.iconUrl('/api/v1/inventory/icons/abc')).toBe('http://localhost:8080/api/v1/inventory/icons/abc');
  });
});
