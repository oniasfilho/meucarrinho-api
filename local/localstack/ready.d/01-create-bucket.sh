#!/bin/sh
# Runs inside the LocalStack container once S3 is ready.
# Creates the bucket the API expects so a fresh `docker compose up` is immediately usable.
awslocal s3 mb s3://meucarrinho-media 2>/dev/null || true
