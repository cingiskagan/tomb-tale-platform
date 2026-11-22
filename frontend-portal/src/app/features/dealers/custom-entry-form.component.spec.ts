import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { Subject, of, throwError } from 'rxjs';
import { DealerService, ItemCatalogService, ItemTemplateResponse, ItemType, Rarity, StandEntry } from '../../core/api';
import { CustomEntryFormComponent } from './custom-entry-form.component';

const RING = { publicId: 't-ring', code: 'RING_GOLD', name: 'Gold Ring', itemType: ItemType.RING } as ItemTemplateResponse;

describe('CustomEntryFormComponent', () => {
  let dealerService: jasmine.SpyObj<DealerService>;
  let fixture: ComponentFixture<CustomEntryFormComponent>;
  let form: CustomEntryFormComponent;

  beforeEach(() => {
    dealerService = jasmine.createSpyObj<DealerService>('DealerService', ['addEntry']);
    const itemCatalogService = jasmine.createSpyObj<ItemCatalogService>('ItemCatalogService', ['getBonusRanges']);
    itemCatalogService.getBonusRanges.and.returnValue(of({ RING: { FIRE_DAMAGE: { min: 1, max: 8 } } }));
    TestBed.configureTestingModule({
      imports: [CustomEntryFormComponent],
      providers: [
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
        { provide: DealerService, useValue: dealerService },
        { provide: ItemCatalogService, useValue: itemCatalogService },
      ],
    });
    fixture = TestBed.createComponent(CustomEntryFormComponent);
    form = fixture.componentInstance;
    form.dealerCode = 'TOWN_JEWELER';
    form.template = RING;
    form.visible = true;
  });

  it('adds the template with the rarity and the exact bonus, a stat twice too, then closes', async () => {
    dealerService.addEntry.and.returnValue(of({ publicId: 'e-1' } as StandEntry));
    const saved = spyOn(form.saved, 'emit');
    form.form.controls.rarity.setValue(Rarity.PURPLE);
    form.form.controls.bonus.setValue([
      { stat: 'FIRE_DAMAGE', value: 40 },
      { stat: 'CRIT_CHANCE', value: 2 },
      { stat: 'FIRE_DAMAGE', value: 40 },
    ]);

    expect(form.form.valid).toBeTrue();
    await form.save();

    expect(dealerService.addEntry).toHaveBeenCalledWith('TOWN_JEWELER', {
      templateId: 't-ring',
      rarity: Rarity.PURPLE,
      bonus: [
        { stat: 'FIRE_DAMAGE', value: 40 },
        { stat: 'CRIT_CHANCE', value: 2 },
        { stat: 'FIRE_DAMAGE', value: 40 },
      ],
    });
    expect(saved).toHaveBeenCalled();
    expect(form.visible).toBeFalse();
  });

  it("stays open with inventory's reason when it refuses", async () => {
    dealerService.addEntry.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 403, error: { detail: 'Custom stand items are off' } })),
    );
    form.form.controls.rarity.setValue(Rarity.GRAY);

    await form.save();

    expect(form.error).toBe('Custom stand items are off');
    expect(form.visible).toBeTrue();
  });

  /** Pressing Save, or Enter in a stat field, twice adds the entry once. */
  it('sends one entry when saved twice before the first answers', async () => {
    const answer = new Subject<StandEntry>();
    dealerService.addEntry.and.returnValue(answer);
    form.form.controls.rarity.setValue(Rarity.GRAY);

    const first = form.save();
    await form.save();
    answer.next({ publicId: 'e-1' } as StandEntry);
    answer.complete();
    await first;

    expect(dealerService.addEntry).toHaveBeenCalledTimes(1);
  });

  it('opens blank each time, with the five empty rows of a red item', async () => {
    form.form.controls.rarity.setValue(Rarity.GREEN);
    form.form.controls.bonus.setValue([{ stat: 'FIRE_DAMAGE', value: 40 }]);

    form.close();
    form.visible = true;

    expect(form.form.controls.rarity.value).toBe(Rarity.RED);
    expect(form.form.controls.bonus.getRawValue()).toEqual(Array(5).fill({ stat: null, value: null }));
  });

  /** Inventory refuses a bonus with more or fewer lines than the rarity rolls, so the admin can't add or remove one. */
  it('gives the bonus as many rows as the rarity rolls, keeping the filled ones', async () => {
    form.form.controls.rarity.setValue(Rarity.BLUE);
    form.form.controls.bonus.at(0).setValue({ stat: 'FIRE_DAMAGE', value: 40 });
    expect(form.form.controls.bonus.length).toBe(2);

    form.form.controls.rarity.setValue(Rarity.ORANGE);
    expect(form.form.controls.bonus.length).toBe(4);
    expect(form.form.controls.bonus.at(0).getRawValue()).toEqual({ stat: 'FIRE_DAMAGE', value: 40 });

    form.form.controls.rarity.setValue(Rarity.GRAY);
    expect(form.form.controls.bonus.length).toBe(0);

    form.form.controls.rarity.setValue(Rarity.GREEN);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(document.querySelectorAll('.stat-row').length).toBe(1);
    expect(document.querySelector('[aria-label="Remove stat"]')).toBeNull();
    expect(Array.from(document.querySelectorAll('button')).some((b) => b.textContent?.includes('Add stat'))).toBeFalse();
  });

  /** A ring rolls fire damage from 1 to 8 and no MAX_HP in these ranges, and 40 is allowed though past them. */
  it("shows a picked stat's roll range for the item type, and takes any value of at least 1", async () => {
    form.form.controls.rarity.setValue(Rarity.GREEN);
    const row = form.form.controls.bonus.at(0);
    row.setValue({ stat: 'FIRE_DAMAGE', value: 0 });
    expect(form.form.valid).toBeFalse();
    row.controls.value.setValue(40);
    expect(form.form.valid).toBeTrue();

    // The ranges load when the dialog opens.
    await fixture.whenStable();
    fixture.detectChanges();
    expect(document.querySelector('mat-hint')?.textContent).toContain('Rolls 1–8');

    row.controls.stat.setValue('MAX_HP');
    fixture.detectChanges();
    expect(document.querySelector('mat-hint')?.textContent).toContain('Never rolls on this item');
  });
});
