package com.howellyoung.exchange.messaging;

public interface Messaging {

    enum Topic {

        /**
         * Topic name: to sequence.
         */
        SEQUENCE(1),

        /**
         * Topic name: to/from trading-engine.
         */
        TRANSFER(1),

        /**
         * Topic name: events to trading-engine.
         */
        TRADE(1),

        /**
         * Topic name: tick to quotation for generate bars.
         */
        TICK(1);

        private final int partitions;

        Topic(int partitions) {
            this.partitions = partitions;
        }

        public int getPartitions() {
            return this.partitions;
        }
    }
}
