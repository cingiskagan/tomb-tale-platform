import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { DealerResponse, DealerService } from '../../core/api';
import { DealerFormComponent } from './dealer-form.component';

function aDealer(name: string, version: number): DealerResponse {
  return { publicId: 'd-1', code: 'TOWN_BLACKSMITH', name, retired: false, version, createdAt: '', updatedAt: '' };
}

describe('DealerFormComponent', () => {
  let dealerService: jasmine.SpyObj<DealerService>;
  let fixture: ComponentFixture<DealerFormComponent>;
  let form: DealerFormComponent;

  beforeEach(() => {
    dealerService = jasmine.createSpyObj<DealerService>('DealerService', ['getDealer', 'createDealer', 'updateDealer']);
    TestBed.configureTestingModule({
      imports: [DealerFormComponent],
      providers: [provideNoopAnimations(), { provide: DealerService, useValue: dealerService }],
    });
    fixture = TestBed.createComponent(DealerFormComponent);
    form = fixture.componentInstance;
  });

  async function open(publicId: string | null) {
    form.dealerPublicId = publicId;
    form.visible = true;
    await fixture.whenStable();
  }

  it('creates a dealer with its code and name', async () => {
    dealerService.createDealer.and.returnValue(of(aDealer('Blacksmith', 0)));
    await open(null);

    form.form.setValue({ code: 'TOWN_BLACKSMITH', name: 'Blacksmith' });
    await form.save();

    expect(dealerService.createDealer).toHaveBeenCalledWith({ code: 'TOWN_BLACKSMITH', name: 'Blacksmith' });
    expect(form.visible).toBeFalse();
  });

  it('fixes the code, and reloads the dealer and says so when someone else saved it first', async () => {
    dealerService.getDealer.and.returnValues(of(aDealer('Blacksmith', 0)), of(aDealer('Old Blacksmith', 1)));
    dealerService.updateDealer.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    await open('d-1');
    expect(form.form.controls.code.disabled).toBeTrue();

    form.form.controls.name.setValue('Young Blacksmith');
    await form.save();

    expect(dealerService.updateDealer).toHaveBeenCalledWith('d-1', { name: 'Young Blacksmith', version: 0 });
    expect(form.form.controls.name.value).toBe('Old Blacksmith');
    expect(form.notice?.severity).toBe('warn');
    expect(form.visible).toBeTrue();
  });
});
