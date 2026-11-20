import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { Subject, of } from 'rxjs';
import { DealerResponse, DealerService, InventoryService, ItemCatalogService, PagedResponse } from '../../core/api';
import { ConfirmService, NotificationService } from '../../core/ui';
import { DealerListComponent } from './dealer-list.component';

function aDealer(code: string): DealerResponse {
  return { publicId: `id-${code}`, code, name: code, retired: false, version: 0, createdAt: '', updatedAt: '' };
}

function aPage(dealers: DealerResponse[]): PagedResponse<DealerResponse> {
  return { content: dealers, page: { number: 0, size: 20, totalElements: dealers.length, totalPages: 1 } };
}

describe('DealerListComponent', () => {
  let dealerService: jasmine.SpyObj<DealerService>;
  let confirm: jasmine.SpyObj<ConfirmService>;
  let list: DealerListComponent;

  beforeEach(() => {
    dealerService = jasmine.createSpyObj<DealerService>('DealerService', ['listDealers', 'retireDealer']);
    confirm = jasmine.createSpyObj<ConfirmService>('ConfirmService', ['ask']);
    confirm.ask.and.resolveTo(true);
    TestBed.configureTestingModule({
      imports: [DealerListComponent],
      providers: [
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
        { provide: DealerService, useValue: dealerService },
        { provide: ConfirmService, useValue: confirm },
        { provide: NotificationService, useValue: jasmine.createSpyObj('NotificationService', ['success', 'error']) },
        { provide: ItemCatalogService, useValue: jasmine.createSpyObj('ItemCatalogService', ['listTemplates']) },
        { provide: InventoryService, useValue: jasmine.createSpyObj('InventoryService', ['iconUrl']) },
      ],
    });
    list = TestBed.createComponent(DealerListComponent).componentInstance;
  });

  /** A second retirement waits until the first one's reload ends, so a double click sends one request. */
  it('takes no other retirement until the reload after the first ends', async () => {
    dealerService.retireDealer.and.returnValue(of(aDealer('TOWN_SMITH')));
    const reload = new Subject<PagedResponse<DealerResponse>>();
    dealerService.listDealers.and.returnValue(reload);

    const first = list.confirmRetire(aDealer('TOWN_SMITH'));
    await new Promise((resolve) => setTimeout(resolve));
    await list.confirmRetire(aDealer('TOWN_TAILOR'));

    expect(dealerService.retireDealer).toHaveBeenCalledTimes(1);
    expect(list.busy).toBeTrue();
    reload.next(aPage([]));
    reload.complete();
    await first;
    expect(list.busy).toBeFalse();
  });
});
