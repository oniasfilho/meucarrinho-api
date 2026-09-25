# Everyday commands (spec §14). Later sessions add seed, token, openapi, logs and swap.
.PHONY: up down reset test it

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
