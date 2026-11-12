import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { DealerService, ItemTemplateResponse, ItemType, Rarity, StandEntry } from '../../core/api';
import { statRow } from '../items/stats-form';
import { CustomEntryFormComponent } from './custom-entry-form.component';

const RING = { publicId: 't-ring', code: 'RING_GOLD', name: 'Gold Ring', itemType: ItemType.RING } as ItemTemplateResponse;

describe('CustomEntryFormComponent', () => {
  let dealerService: jasmine.SpyObj<DealerService>;
  let fixture: ComponentFixture<CustomEntryFormComponent>;
  let form: CustomEntryFormComponent;

  beforeEach(() => {
    dealerService = jasmine.createSpyObj<DealerService>('DealerService', ['addEntry']);
    TestBed.configureTestingModule({
      imports: [CustomEntryFormComponent],
      providers: [provideNoopAnimations(), { provide: DealerService, useValue: dealerService }],
    });
    fixture = TestBed.createComponent(CustomEntryFormComponent);
    form = fixture.componentInstance;
    form.dealerCode = 'TOWN_JEWELER';
    form.template = RING;
    form.visible = true;
  });

  it('adds the template with the rarity and the exact bonus, then closes', async () => {
    dealerService.addEntry.and.returnValue(of({ publicId: 'e-1' } as StandEntry));
    const saved = spyOn(form.saved, 'emit');
    form.form.controls.rarity.setValue(Rarity.ORANGE);
    form.form.controls.bonus.push(statRow('FIRE_DAMAGE', 40));
    form.form.controls.bonus.push(statRow('CRIT_CHANCE', 2));

    await form.save();

    expect(dealerService.addEntry).toHaveBeenCalledWith('TOWN_JEWELER', {
      templateId: 't-ring',
      rarity: Rarity.ORANGE,
      bonus: { FIRE_DAMAGE: 40, CRIT_CHANCE: 2 },
    });
    expect(saved).toHaveBeenCalled();
    expect(form.visible).toBeFalse();
  });

  it("stays open with inventory's reason when it refuses", async () => {
    dealerService.addEntry.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 403, error: { detail: 'Custom stand items are off' } })),
    );

    await form.save();

    expect(form.error).toBe('Custom stand items are off');
    expect(form.visible).toBeTrue();
  });

  it('opens blank each time', async () => {
    form.form.controls.bonus.push(statRow('FIRE_DAMAGE', 40));
    form.form.controls.rarity.setValue(Rarity.GREEN);

    form.close();
    form.visible = true;

    expect(form.form.controls.bonus.length).toBe(0);
    expect(form.form.controls.rarity.value).toBe(Rarity.RED);
  });
});
