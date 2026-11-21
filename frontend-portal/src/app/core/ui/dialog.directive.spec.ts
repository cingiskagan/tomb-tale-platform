import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogDirective } from './dialog.directive';

@Component({
  standalone: true,
  imports: [DialogDirective],
  template: `
    <ng-template [appDialog]="open" appDialogWidth="20rem" (appDialogDismissed)="dismissed = dismissed + 1">
      <p class="body">Hello {{ name }}</p>
    </ng-template>
  `,
})
class HostComponent {
  open = false;
  dismissed = 0;
  name = 'Aria';
}

describe('DialogDirective', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [{ provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } }],
    });
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
  });

  afterEach(() => fixture.destroy());

  async function setOpen(open: boolean) {
    host.open = open;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function body(): HTMLElement | null {
    return document.querySelector('.body');
  }

  /** The template shows in a dialog, bound to the host, only while the host says open. */
  it('opens the template while the host says open, and closes it when the host says closed', async () => {
    await setOpen(true);
    expect(body()?.textContent).toContain('Hello Aria');

    host.name = 'Bram';
    fixture.detectChanges();
    expect(body()?.textContent).toContain('Hello Bram');

    await setOpen(false);
    expect(body()).toBeNull();
    expect(host.dismissed).toBe(0);
  });

  /** A close the host did not ask for, as Escape or a navigation does, reaches the host once. */
  it('tells the host when the dialog closes on its own', async () => {
    await setOpen(true);

    TestBed.inject(MatDialog).closeAll();
    await fixture.whenStable();

    expect(host.dismissed).toBe(1);
    expect(body()).toBeNull();
  });

  /** A host that goes away takes its dialog with it. */
  it('closes the dialog when the host is destroyed', async () => {
    await setOpen(true);

    fixture.destroy();
    await new Promise((resolve) => setTimeout(resolve));

    expect(TestBed.inject(MatDialog).openDialogs.length).toBe(0);
  });
});
