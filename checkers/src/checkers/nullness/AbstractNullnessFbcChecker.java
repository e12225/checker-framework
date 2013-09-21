package checkers.nullness;

import java.util.Collection;
import java.util.HashSet;

import checkers.initialization.quals.Initialized;
import checkers.initialization.quals.FBCBottom;
import checkers.initialization.quals.UnderInitialization;
import checkers.initialization.quals.UnknownInitialization;
import checkers.nullness.quals.MonotonicNonNull;
import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.PolyNull;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.source.SupportedLintOptions;

/**
 * A concrete instantiation of {@link AbstractNullnessChecker} using
 * freedom-before-commitment.
 */
@TypeQualifiers({ Nullable.class, MonotonicNonNull.class, NonNull.class,
        UnderInitialization.class, Initialized.class, UnknownInitialization.class,
        FBCBottom.class, PolyNull.class, PolyAll.class })
@SupportedLintOptions({
        AbstractNullnessChecker.LINT_NOINITFORMONOTONICNONNULL,
        AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON,
        // Temporary option to forbid non-null array component types,
        // which is allowed by default.
        // Forbidding is sound and will eventually be the only possibility.
        // Allowing is unsound but permitted until flow-sensitivity changes are
        // made.
        "arrays:forbidnonnullcomponents" })
public class AbstractNullnessFbcChecker extends AbstractNullnessChecker {

    public AbstractNullnessFbcChecker() {
        super(true);
    }

    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Collection<String> result = new HashSet<>(super.getSuppressWarningsKeys());
        result.add("fbc");
        return result;
    }

}