# Everyday commands (spec §14). Later sessions add openapi, logs and swap.
.PHONY: up down reset test it deps run token

# The person `make token` signs in as: marina, jessica or onias from the demo seed, or any new name.
user ?= marina

up:     ## Start the local stack, including the API, and wait until it is healthy
	docker compose --profile full up --build -d --wait

down:   ## Stop the local stack, keeping volumes
	docker compose --profile full --profile offline down

reset:  ## Wipe volumes and start clean
	docker compose --profile full --profile offline down -v
	$(MAKE) up

test:   ## Unit, use-case and architecture tests with in-memory fakes; no Docker
	./gradlew test

it:     ## Integration and contract tests on Testcontainers
	./gradlew integrationTest

deps:   ## Start what the API needs on this machine: Postgres, Valkey and mock-oauth2 (sign-in)
	docker compose --profile offline up -d --wait postgres valkey mock-oauth2

run: deps  ## Run the API on :8080 with the demo data (loaded once, through the use cases)
	./gradlew bootRun --args='--carrinho.seed.enabled=true'

token:  ## Print an access token from mock-oauth2: make token user=jessica
	@curl -sSf -X POST http://localhost:8090/default/token \
		-d grant_type=client_credentials -d client_id=$(user) -d client_secret=local -d scope=carrinho-api \
		| tr -d ' \n' | sed -E 's/.*"access_token":"([^"]+)".*/\1/'; echo
