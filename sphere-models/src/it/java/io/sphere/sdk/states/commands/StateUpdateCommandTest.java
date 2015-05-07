package io.sphere.sdk.states.commands;

import io.sphere.sdk.models.LocalizedStrings;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.commands.updateactions.ChangeInitial;
import io.sphere.sdk.states.commands.updateactions.ChangeKey;
import io.sphere.sdk.states.commands.updateactions.SetName;
import io.sphere.sdk.states.commands.updateactions.SetTransitions;
import io.sphere.sdk.test.IntegrationTest;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static io.sphere.sdk.states.StateFixtures.withUpdateableState;
import static io.sphere.sdk.test.SphereTestUtils.*;
import static io.sphere.sdk.utils.SetUtils.asSet;
import static org.fest.assertions.Assertions.assertThat;
import static io.sphere.sdk.test.OptionalAssert.assertThat;

public class StateUpdateCommandTest extends IntegrationTest {
    @Test
    public void changeInitial() throws Exception {
        withUpdateableState(client(), state -> {
            final boolean oldIsInitial = state.isInitial();
            final boolean newIsInitial = !oldIsInitial;

            final StateUpdateCommand command = StateUpdateCommand.of(state, ChangeInitial.of(newIsInitial));
            final State updatedState = execute(command);
            assertThat(updatedState.isInitial()).isEqualTo(newIsInitial);
            return updatedState;
        });
    }

    @Test
    public void changeKey() throws Exception {
        withUpdateableState(client(), state -> {
            final String newKey = randomKey();

            final StateUpdateCommand command = StateUpdateCommand.of(state, ChangeKey.of(newKey));
            final State updatedState = execute(command);
            assertThat(updatedState.getKey()).isEqualTo(newKey);
            return updatedState;
        });
    }

    @Test
    public void setName() throws Exception {
        withUpdateableState(client(), state -> {
            final LocalizedStrings newName = randomSlug();

            final StateUpdateCommand command = StateUpdateCommand.of(state, SetName.of(newName));
            final State updatedState = execute(command);
            assertThat(updatedState.getName()).isPresentAs(newName);
            return updatedState;
        });
    }

    @Test
    public void setTransitions() throws Exception {
        withUpdateableState(client(), stateA -> {
            withUpdateableState(client(), stateB -> {
                final Set<Reference<State>> transitions = asSet(stateA.toReference());
                final StateUpdateCommand command = StateUpdateCommand.of(stateB, SetTransitions.of(transitions));
                final State updatedStateB = execute(command);
                assertThat(updatedStateB.getTransitions()).isPresentAs(transitions);

                final State updatedStateBWithoutTransitions = execute(StateUpdateCommand.of(updatedStateB, SetTransitions.of(Optional.empty())));
                assertThat(updatedStateBWithoutTransitions.getTransitions()).isAbsent();

                return updatedStateBWithoutTransitions;
            });
            return stateA;
        });
    }
}