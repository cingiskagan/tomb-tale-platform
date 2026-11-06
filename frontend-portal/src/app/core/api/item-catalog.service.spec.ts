import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RUNTIME_CONFIG, RuntimeConfig } from '../config';
import { ItemCatalogService } from './item-catalog.service';

const CONFIG: RuntimeConfig = {
  issuerUri: 'http://localhost:8080/realms/tombtale',
  clientId: 'test-client',
  apiBaseUrl: 'http://localhost:8080',
};

const INVENTORY = 'http://localhost:8080/api/v1/inventory';

describe('ItemCatalogService', () => {
  let service: ItemCatalogService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: RUNTIME_CONFIG, useValue: CONFIG }],
    });
    service = TestBed.inject(ItemCatalogService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists templates with the page, the size and the sort', () => {
    service.listTemplates(2, 10, 'name,desc').subscribe();

    const request = http.expectOne((req) => req.url === `${INVENTORY}/templates`);
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.get('sort')).toBe('name,desc');
    request.flush({ content: [], page: { number: 2, size: 10, totalElements: 0, totalPages: 0 } });
  });

  it('sends an edit with the version it started from', () => {
    service
      .updateTemplate('t-1', {
        name: 'Iron Sword',
        price: 150,
        baseStats: { PHYSICAL_DAMAGE: 12 },
        assetKey: 'Items/SwordIron',
        iconHash: null,
        itemSetId: null,
        version: 3,
      })
      .subscribe();

    const request = http.expectOne(`${INVENTORY}/templates/t-1`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.version).toBe(3);
    request.flush({});
  });

  it('retires a template with a POST and no body', () => {
    service.retireTemplate('t-1').subscribe();

    const request = http.expectOne(`${INVENTORY}/templates/t-1/retire`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush({});
  });

  it('uploads an icon as the multipart field inventory reads', () => {
    const file = new File([new Uint8Array([0x89, 0x50])], 'sword.png', { type: 'image/png' });

    service.uploadIcon(file).subscribe();

    const request = http.expectOne(`${INVENTORY}/icons`);
    expect((request.request.body as FormData).get('file')).toBe(file);
    request.flush({ hash: 'abc', contentType: 'image/png' });
  });

  it('points an icon URL at its hash', () => {
    expect(service.iconUrl('abc')).toBe(`${INVENTORY}/icons/abc`);
  });
});
