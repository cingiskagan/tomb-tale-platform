import { TestBed } from '@angular/core/testing';
import { ChangeBlockerComponent } from './change-blocker.component';
import { LoadingService } from './loading.service';

describe('ChangeBlockerComponent', () => {
  let loading: LoadingService;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ChangeBlockerComponent] });
    loading = TestBed.inject(LoadingService);
  });

  /** Presses Enter on a button and tells whether the button saw it. */
  function pressEnter(): boolean {
    const button = document.createElement('button');
    document.body.appendChild(button);
    let seen = false;
    button.addEventListener('keydown', () => (seen = true));
    button.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true }));
    button.remove();
    return seen;
  }

  /** While a change runs, the cover is drawn and Enter never reaches a control. */
  it('covers the portal and swallows Enter while a change runs', () => {
    const fixture = TestBed.createComponent(ChangeBlockerComponent);
    loading.start(true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.change-blocker')).not.toBeNull();
    expect(pressEnter()).toBeFalse();
    fixture.destroy();
  });

  /** Without a change, and after the blocker is gone, keys pass as usual. */
  it('lets keys through when no change runs, and after it is destroyed', () => {
    const fixture = TestBed.createComponent(ChangeBlockerComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.change-blocker')).toBeNull();
    expect(pressEnter()).toBeTrue();

    loading.start(true);
    fixture.destroy();
    expect(pressEnter()).toBeTrue();
  });
});
