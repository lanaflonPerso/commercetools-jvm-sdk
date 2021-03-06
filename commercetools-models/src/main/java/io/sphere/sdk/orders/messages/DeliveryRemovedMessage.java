package io.sphere.sdk.orders.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.sphere.sdk.messages.GenericMessageImpl;
import io.sphere.sdk.messages.MessageDerivateHint;
import io.sphere.sdk.messages.UserProvidedIdentifiers;
import io.sphere.sdk.orders.Delivery;
import io.sphere.sdk.orders.Order;

import java.time.ZonedDateTime;

/**
 * This message is the result of the {@link io.sphere.sdk.orders.commands.updateactions.RemoveDelivery} update action.
 *
 * {@include.example io.sphere.sdk.orders.commands.OrderUpdateCommandIntegrationTest#addDelivery()}
 *
 */
@JsonDeserialize(as = DeliveryRemovedMessage.class)//important to override annotation in Message class
public final class DeliveryRemovedMessage extends GenericMessageImpl<Order> implements SimpleOrderMessage {
    public static final String MESSAGE_TYPE = "DeliveryRemoved";
    public static final MessageDerivateHint<DeliveryRemovedMessage> MESSAGE_HINT = MessageDerivateHint.ofSingleMessageType(MESSAGE_TYPE, DeliveryRemovedMessage.class, Order.referenceTypeId());

    private final Delivery delivery;


    @JsonCreator
    private DeliveryRemovedMessage(final String id, final Long version, final ZonedDateTime createdAt,
                                   final ZonedDateTime lastModifiedAt, final JsonNode resource, final Long sequenceNumber,
                                   final Long resourceVersion, final String type, final UserProvidedIdentifiers resourceUserProvidedIdentifiers, final Delivery delivery) {
        super(id, version, createdAt, lastModifiedAt, resource, sequenceNumber, resourceVersion, type,resourceUserProvidedIdentifiers, Order.class);
        this.delivery = delivery;
    }

    public Delivery getDelivery() {
        return delivery;
    }
}
