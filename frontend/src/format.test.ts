import {describe,it,expect} from 'vitest';
import {money,percent,monthBounds,csvCell} from './format';
describe('financial display and export',()=>{
 it('preserves decimal precision for large amounts',()=>expect(money('9999999999999.99','USD',2)).toBe('$9,999,999,999,999.99'));
 it('handles negative and zero values',()=>{expect(money('-10.25','EUR',2)).toBe('-€10.25');expect(money('0','USD',2)).toBe('$0.00');});
 it('does not invent undefined percentages',()=>{expect(percent(null)).toBe('—');expect(percent('12.34')).toBe('+12.3%');});
 it('calculates leap-year and year-boundary filters',()=>{expect(monthBounds('2024-02').end).toBe('2024-02-29');expect(monthBounds('2026-12').end).toBe('2026-12-31');});
 it('escapes CSV formulas, newlines and quotes',()=>{expect(csvCell('=SUM(A1)')).toBe('"\'=SUM(A1)"');expect(csvCell('a,"b"\nc')).toBe('"a,""b""\nc"');});
});
