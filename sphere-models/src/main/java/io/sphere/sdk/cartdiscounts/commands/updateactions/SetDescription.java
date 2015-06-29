package io.sphere.sdk.cartdiscounts.commands.updateactions;

import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedStrings;

import java.util.Optional;

/**
 * {@include.example io.sphere.sdk.cartdiscounts.commands.CartDiscountUpdateCommandTest#setDescription()}
 */
public class SetDescription extends UpdateAction<CartDiscount> {
    private final Optional<LocalizedStrings> description;

    private SetDescription(final Optional<LocalizedStrings> description) {
        super("setDescription");
        this.description = description;
    }

    public static SetDescription of(final Optional<LocalizedStrings> description) {
        return new SetDescription(description);
    }

    public static SetDescription of(final LocalizedStrings description) {
        return of(Optional.of(description));
    }

    public Optional<LocalizedStrings> getDescription() {
        return description;
    }
}
