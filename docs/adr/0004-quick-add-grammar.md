# 0004. Quick-add grammar

Date: 2026-09-25 · Status: accepted

## Context

§11 fixes three examples ("2 leite 5,49", "1,2 kg tomate", "cafe") and requires the app
and the backend to parse identically, but does not define the grammar.

## Decision

- A leading number is the quantity only if a name follows it. Forms: `2`, `3x`, `2 un`,
  `1,2 kg`, `0,5kg`. Without `kg` the unit is `UN`. The quantity must be valid for its unit,
  otherwise `INVALID_QUANTITY` (so "1,5 leite" is rejected rather than guessed as kg).
- A trailing amount is a price only if it has decimals or an `R$`: `5,49`, `19.9`,
  `R$ 24`, `R$24,90`. A bare trailing integer stays in the name ("vitamina c 12").
- The rest is the name. Empty input is `EMPTY`.

The rules live in `domain.list.QuickAddParser`; the vectors in
`contracts/quick-add-vectors.json` are the contract the app pins.

## Consequences

Conservative on purpose: when in doubt, text stays in the name, which the user can see
and fix, instead of silently becoming a price or quantity.
