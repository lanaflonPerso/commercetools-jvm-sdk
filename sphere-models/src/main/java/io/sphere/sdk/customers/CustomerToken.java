package io.sphere.sdk.customers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.sphere.sdk.models.Timestamped;

/**
 * A token belonging to a customer to verify an email address or resetting the password.
 *
 * @see Customer
 * @see io.sphere.sdk.customers.queries.CustomerByTokenGet
 * @see io.sphere.sdk.customers.commands.CustomerCreateEmailTokenCommand
 * @see io.sphere.sdk.customers.commands.CustomerCreatePasswordTokenCommand
 * @see io.sphere.sdk.customers.commands.CustomerPasswordResetCommand
 * @see io.sphere.sdk.customers.commands.CustomerVerifyEmailCommand
 */
@JsonDeserialize(as = CustomerTokenImpl.class)
public interface CustomerToken extends Timestamped {
    /**
     * The ID of the token.
     * @return the id of the token
     */
    String getId();

    /**
     * The ID of the customer belonging to the token
     * @return customer id
     */
    String getCustomerId();

    /**
     * the token value
     * @return secret token
     */
    String getValue();

    static TypeReference<CustomerToken> typeReference() {
        return new TypeReference<CustomerToken>() {
            @Override
            public String toString() {
                return "TypeReference<CustomerToken>";
            }
        };
    }
}
