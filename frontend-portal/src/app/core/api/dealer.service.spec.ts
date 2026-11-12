import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RUNTIME_CONFIG, RuntimeConfig } from '../config';
import { DealerService } from './dealer.service';
import { Rarity } from './inventory.models';

const CONFIG: RuntimeConfig = {
  issuerUri: 'http://localhost:8080/realms/tombtale',
  clientId: 'test-client',
  apiBaseUrl: 'http://localhost:8080',
  production: false,
};

const INVENTORY = 'http://localhost:8080/api/v1/inventory';

describe('DealerService', () => {
  let service: DealerService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: RUNTIME_CONFIG, useValue: CONFIG }],
    });
    service = TestBed.inject(DealerService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists dealers with the page, the size and the sort', () => {
    service.listDealers(1, 10, 'name,desc').subscribe();

    const request = http.expectOne((req) => req.url === `${INVENTORY}/dealers`);
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('sort')).toBe('name,desc');
  });

  it('retires a dealer with a POST and no body', () => {
    service.retireDealer('d-1').subscribe();

    const request = http.expectOne(`${INVENTORY}/dealers/d-1/retire`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
  });

  it("reads a stand by its dealer's code", () => {
    service.getStand('TOWN_BLACKSMITH').subscribe();

    expect(http.expectOne(`${INVENTORY}/stands/TOWN_BLACKSMITH`).request.method).toBe('GET');
  });

  it('adds a custom entry with its bonus, and removes an entry by its id', () => {
    service.addEntry('TOWN_BLACKSMITH', { templateId: 't-1', rarity: Rarity.RED, bonus: { FIRE_DAMAGE: 40 } }).subscribe();
    service.removeEntry('TOWN_BLACKSMITH', 'e-1').subscribe();

    const add = http.expectOne(`${INVENTORY}/stands/TOWN_BLACKSMITH/entries`);
    expect(add.request.method).toBe('POST');
    expect(add.request.body).toEqual({ templateId: 't-1', rarity: Rarity.RED, bonus: { FIRE_DAMAGE: 40 } });
    expect(http.expectOne(`${INVENTORY}/stands/TOWN_BLACKSMITH/entries/e-1`).request.method).toBe('DELETE');
  });

  it("reads inventory's settings", () => {
    service.getSettings().subscribe();

    expect(http.expectOne(`${INVENTORY}/settings`).request.method).toBe('GET');
  });
});
