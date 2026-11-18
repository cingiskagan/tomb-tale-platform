import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmService } from './confirm.service';

const RETIRE = { title: 'Retire dealer', message: 'Retire TOWN_SMITH?', accept: 'Retire', reject: 'Cancel' };

describe('ConfirmService', () => {
  let confirm: ConfirmService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } }],
    });
    confirm = TestBed.inject(ConfirmService);
  });

  afterEach(() => TestBed.inject(MatDialog).closeAll());

  function button(label: string): HTMLButtonElement {
    const buttons = Array.from(document.querySelectorAll<HTMLButtonElement>('mat-dialog-actions button'));
    return buttons.find((candidate) => candidate.textContent?.trim() === label)!;
  }

  /** The question and both labels show, and only the accept button answers yes. */
  it('answers true when the admin accepts', async () => {
    const answer = confirm.ask(RETIRE);
    await new Promise((resolve) => setTimeout(resolve));
    expect(document.querySelector('mat-dialog-content')?.textContent).toContain('Retire TOWN_SMITH?');

    button('Retire').click();

    expect(await answer).toBeTrue();
  });

  /** The reject button answers no. */
  it('answers false when the admin rejects', async () => {
    const answer = confirm.ask(RETIRE);
    await new Promise((resolve) => setTimeout(resolve));

    button('Cancel').click();

    expect(await answer).toBeFalse();
  });

  /** Escape, the backdrop or a navigation close the dialog without an answer, which counts as no. */
  it('answers false when the dialog closes any other way', async () => {
    const answer = confirm.ask(RETIRE);

    TestBed.inject(MatDialog).closeAll();

    expect(await answer).toBeFalse();
  });
});
