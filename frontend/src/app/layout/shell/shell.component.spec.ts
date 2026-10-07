import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ShellComponent } from './shell.component';

describe('ShellComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [provideRouter([])],
    });
  });

  it('muestra un enlace por cada módulo', async () => {
    const fixture = TestBed.createComponent(ShellComponent);
    await fixture.whenStable();

    const enlaces = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLAnchorElement>('mat-nav-list a'),
    );

    expect(enlaces.map((a) => a.getAttribute('href'))).toEqual([
      '/pacientes',
      '/trabajadores',
      '/medicamentos',
      '/historial',
    ]);
  });
});
