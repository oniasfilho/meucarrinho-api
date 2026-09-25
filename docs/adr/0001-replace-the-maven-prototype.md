# 0001. Replace the Maven prototype in place

Date: 2026-09-25 · Status: accepted

## Context

`oniasfilho/meucarrinho-api` already held a first implementation: Maven, one package per
feature (`br.com.oniasfilho.meucarrinho.session`, `user`), JPA entities, controllers and
a LocalStack-based photo upload. The spec asks for one Gradle module under
`app.meucarrinho` with a hexagonal layout.

## Decision

Session 1 replaces the Maven project on a branch in the same repository instead of
starting a new `carrinho-backend` repository. The old code stays in the history of `main`
(commit `0b49201`). The Dockerfile now builds with Gradle; the Cloud Run workflow is left
untouched.

## Consequences

- One repository and one deploy pipeline, as before.
- The session 1 build has no web server, so the Cloud Run deploy that runs on every push
  to `main` would start a process that exits. Merge this work together with session 2, or
  switch the workflow to manual runs first.
