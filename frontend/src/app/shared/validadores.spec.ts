import { FormControl } from '@angular/forms';
import { obligatorio } from './validadores';

describe('obligatorio', () => {
  it.each([null, undefined, '', '   '])('rechaza %j', (valor) => {
    expect(obligatorio(new FormControl(valor))).toEqual({ obligatorio: true });
  });

  it.each(['admin', ' a ', 0])('acepta %j', (valor) => {
    expect(obligatorio(new FormControl(valor))).toBeNull();
  });
});
