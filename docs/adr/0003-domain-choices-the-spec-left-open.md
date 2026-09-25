# 0003. Domain choices the spec left open

Date: 2026-09-25 · Status: accepted

Each of these is small and easy to change later; they are listed so none is a surprise.

| Topic | Choice | Why |
|---|---|---|
| List name | 1-80 characters, trimmed, inner spaces collapsed | Same limit as store names; §4 gives none |
| Subtotal rounding | quantity x unit price, rounded half-up to the centavo | What a till does with 1.2 kg x R$ 8,90 |
| Over-budget event | `BudgetExceeded` fires when the picked total crosses the budget, once per crossing | §9 says "picked total passes budget"; repeating it on every pick would spam listeners |
| Totals and unpriced items | Items without a price count as zero and are counted in `ListTotals.unpricedCount` | The app can show "2 itens sem preço" instead of a wrong total |
| Restore after delete | A list returns to the status it had before deletion | The diagram in §4 shows `DELETED -> ACTIVE`, which would reopen finished trips |
| Member limit | 10 people including the owner | §4 says "max 10 members" |
| Default preferences | Sort `ADDED`, collaboration alerts on, haptics on, analytics opted out | Only analytics has a stated default (§9) |
| Who finishes | Owner and any member, guests included (§15) | As decided |
| Device actor | `ActorRef.DeviceActor` carries an `InstallId` | §4 says `DeviceId`, §5 says `InstallId`; one install identifier is enough |
| Retries | Creating a list with the same client ID, adding an item with the same ID, and finishing with the same receipt ID all return the earlier result | Safe retries on flaky connections, as §8 asks of sync |
| Results of list use cases | Every list and item use case returns the saved `ShoppingList` | Clients need the new version for `If-Match` and fresh totals after every change |
| Receipt history months | Cut in `America/Sao_Paulo` unless the caller passes a zone | Brazil-only product; the API can pass the user's zone later |
