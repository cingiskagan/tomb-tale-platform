import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ItemCatalogService, ItemTemplateResponse, ItemType } from '../../core/api';
import { ItemTemplateFormComponent } from './item-template-form.component';

function aTemplate(name: string, version: number): ItemTemplateResponse {
  return {
    publicId: 't-1',
    code: 'SWORD_IRON',
    name,
    itemType: ItemType.ONE_HANDED_WEAPON,
    maxStackSize: 1,
    price: 150,
    baseStats: { PHYSICAL_DAMAGE: 12 },
    assetKey: 'Items/SwordIron',
    iconHash: null,
    itemSetId: null,
    retired: false,
    version,
    createdAt: '2026-11-06T00:00:00Z',
    updatedAt: '2026-11-06T00:00:00Z',
  };
}

describe('ItemTemplateFormComponent', () => {
  let catalog: jasmine.SpyObj<ItemCatalogService>;
  let fixture: ComponentFixture<ItemTemplateFormComponent>;
  let form: ItemTemplateFormComponent;

  beforeEach(() => {
    catalog = jasmine.createSpyObj<ItemCatalogService>('ItemCatalogService', [
      'getTemplate',
      'createTemplate',
      'updateTemplate',
      'uploadIcon',
      'iconUrl',
    ]);
    TestBed.configureTestingModule({
      imports: [ItemTemplateFormComponent],
      providers: [provideNoopAnimations(), { provide: ItemCatalogService, useValue: catalog }],
    });
    fixture = TestBed.createComponent(ItemTemplateFormComponent);
    form = fixture.componentInstance;
  });

  /** Opening the dialog loads the template, as the page does it. */
  async function open(publicId: string | null) {
    form.templatePublicId = publicId;
    form.visible = true;
    await fixture.whenStable();
  }

  it('fixes the code and the type of a template that exists', async () => {
    catalog.getTemplate.and.returnValue(of(aTemplate('Iron Sword', 0)));

    await open('t-1');

    expect(form.form.controls.code.disabled).toBeTrue();
    expect(form.form.controls.itemType.disabled).toBeTrue();
    expect(form.form.controls.name.value).toBe('Iron Sword');
    expect(form.form.controls.baseStats.getRawValue()).toEqual([{ stat: 'PHYSICAL_DAMAGE', value: 12 }]);
  });

  it('reloads the template and says so when someone else saved it first', async () => {
    catalog.getTemplate.and.returnValues(of(aTemplate('Iron Sword', 0)), of(aTemplate('Their Sword', 1)));
    catalog.updateTemplate.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    const saved = spyOn(form.saved, 'emit');
    await open('t-1');

    form.form.controls.name.setValue('My Sword');
    await form.save();

    expect(catalog.updateTemplate.calls.mostRecent().args[1].version).toBe(0);
    expect(catalog.getTemplate).toHaveBeenCalledTimes(2);
    expect(form.form.controls.name.value).toBe('Their Sword');
    expect(form.notice?.severity).toBe('warn');
    expect(form.visible).toBeTrue();
    expect(saved).not.toHaveBeenCalled();
  });

  it('shows the backend detail when a new template is refused', async () => {
    catalog.createTemplate.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'The code is taken' } })),
    );
    await open(null);
    form.form.patchValue({ code: 'SWORD_IRON', name: 'Iron Sword', itemType: ItemType.RING, assetKey: 'Items/X' });

    await form.save();

    expect(catalog.getTemplate).not.toHaveBeenCalled();
    expect(form.notice).toEqual({ severity: 'error', text: 'The code is taken' });
  });
});
