#!/bin/bash
set -e

# 최초 데이터 초기화시에만 적용됨!
cp /docker-entrypoint-initdb.d/postgresql.conf /var/lib/postgresql/data/
cp /docker-entrypoint-initdb.d/pg_hba.conf /var/lib/postgresql/data/
chown postgres:postgres /var/lib/postgresql/data/postgresql.conf
chown postgres:postgres /var/lib/postgresql/data/pg_hba.conf
