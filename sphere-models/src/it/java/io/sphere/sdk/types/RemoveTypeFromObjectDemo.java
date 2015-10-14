package io.sphere.sdk.types;

import io.sphere.sdk.categories.commands.updateactions.SetCustomType;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.CategoryUpdateCommand;
import io.sphere.sdk.client.TestClient;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoveTypeFromObjectDemo {
    public static Category removeTypeFromCategory(final TestClient client, final Category category) {
        assertThat(category.getCustom()).isNotNull();

        final CategoryUpdateCommand command = CategoryUpdateCommand
                .of(category, SetCustomType.ofRemoveType());
        final Category updatedCategory = client.execute(command);

        assertThat(updatedCategory.getCustom()).isNull();

        return updatedCategory;
    }
}
