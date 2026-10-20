#!/usr/bin/env bash
# Creates the topics from REQUIREMENTS.md §5.1, plus a .retry and .dlq topic for each.
# Runs inside the kafka-init-topics container, or from the host (via docker exec on kafka-1).
set -euo pipefail

BOOTSTRAP="${BOOTSTRAP_SERVERS:-kafka-1:19092}"
PARTITIONS="${KAFKA_TOPIC_PARTITIONS:-3}"
REPLICATION="${KAFKA_TOPIC_REPLICATION_FACTOR:-3}"

if [[ -x /opt/kafka/bin/kafka-topics.sh ]]; then
  KAFKA_TOPICS=(/opt/kafka/bin/kafka-topics.sh)
else
  KAFKA_CONTAINER="${KAFKA_CONTAINER:-kafka-1}"
  if ! docker inspect -f '{{.State.Running}}' "$KAFKA_CONTAINER" 2>/dev/null | grep -q true; then
    echo "Container $KAFKA_CONTAINER is not running; start it with: docker compose up -d" >&2
    exit 1
  fi
  KAFKA_TOPICS=(docker exec "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-topics.sh)
fi

TOPICS=(
  payment-request-topic
  payment-response-topic
  restaurant-approval-request-topic
  restaurant-approval-response-topic
  customer-topic
  restaurant-topic
  order-status-topic
)

create() {
  local name="$1" partitions="$2"
  "${KAFKA_TOPICS[@]}" --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
    --topic "$name" --partitions "$partitions" --replication-factor "$REPLICATION"
}

for topic in "${TOPICS[@]}"; do
  create "$topic" "$PARTITIONS"
  create "$topic.retry" "$PARTITIONS"
  create "$topic.dlq" 1
done

"${KAFKA_TOPICS[@]}" --bootstrap-server "$BOOTSTRAP" --list
