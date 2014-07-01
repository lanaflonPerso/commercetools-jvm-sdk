package io.sphere.sdk.producttypes.attributes;

import io.sphere.sdk.models.LocalizedString;

import java.util.List;

class EnumAttributeDefinitionBuilder extends BaseBuilder<EnumAttributeDefinition> {

    private final List<PlainEnumValue> values;

    public EnumAttributeDefinitionBuilder(final String name, final LocalizedString label, final List<PlainEnumValue> values) {
        super(name, label);
        this.values = values;
    }

    @Override
    protected EnumAttributeDefinitionBuilder getThis() {
        return this;
    }

    @Override
    public EnumAttributeDefinition build() {
        return new EnumAttributeDefinition(new EnumType(values), getName(), getLabel(), isRequired(), getAttributeConstraint(), isSearchable());
    }
}
