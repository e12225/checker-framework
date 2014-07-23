package org.checkerframework.framework.type.explicit;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import static org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

import static org.checkerframework.framework.type.explicit.ElementAnnotationUtil.annotateViaTypeAnnoPosition;
import static org.checkerframework.framework.type.explicit.ElementAnnotationUtil.getBoundIndexOffset;
import static com.sun.tools.javac.code.Attribute.TypeCompound;

import com.sun.tools.javac.code.TargetType;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import java.util.*;

/**
 * Applies Element annotations to a single AnnotatedTypeVariable representing a type parameter.
 * Note, the index of IndexedElementAnnotationApplier refers to the type parameter's index in
 * the list that encloses it.
 */
abstract class TypeParamElementAnnotationApplier extends IndexedElementAnnotationApplier {

    /**
     * @return True if element is a TYPE_PARAMETER
     */
    public static boolean accepts(AnnotatedTypeMirror typeMirror, Element element) {
        return element.getKind() == ElementKind.TYPE_PARAMETER;
    }

    protected final AnnotatedTypeVariable typeParam;
    protected final AnnotatedTypeFactory typeFactory;

    /**
     * @return Target type that represents the location of the lower bound of element
     */
    abstract protected TargetType lowerBoundTarget();

    /**
     * @return Target type that represents the location of the upper bound of element
     */
    abstract protected TargetType upperBoundTarget();

    public TypeParamElementAnnotationApplier( final AnnotatedTypeVariable type,
                                              final Element element,
                                              final AnnotatedTypeFactory typeFactory) {
        super(type, element);
        this.typeParam   = type;
        this.typeFactory = typeFactory;
    }

    /**
     * @return The lower bound and upper bound targets
     */
    @Override
    protected TargetType[] annotatedTargets() {
        return new TargetType[]{ lowerBoundTarget(), upperBoundTarget() };
    }

    /**
     * @return The parameter_index of anno's TypeAnnotationPosition which will actually
     * point to the type parameter's index in its enclosing type parameter list
     */
    @Override
    public int getTypeCompoundIndex(final TypeCompound anno) {
        return anno.getPosition().parameter_index;
    }

    /**
     * @param targeted The list of annotations that were on the lower/upper bounds of the type parameter
     *
     * Note: When handling type parameters we NEVER add primary annotations to the type parameter.
     * Primary annotations are reserved for the use of a type parameter (e.g. @Nullable T t; )
     *
     * If an annotation is present on the type parameter itself, it represents the lower-bound annotation
     * of that type parameter.  Any annotation on the extends bound of a type parameter is placed on
     * that bound.
     */
    @Override
    protected void handleTargeted(final List<TypeCompound> targeted) {
        final int paramIndex = getElementIndex();
        final List<TypeCompound> upperBoundAnnos = new ArrayList<>();
        final List<TypeCompound> lowerBoundAnnos = new ArrayList<>();


        for( final TypeCompound anno : targeted) {
            final AnnotationMirror aliasedAnno = typeFactory.aliasedAnnotation(anno);
            final AnnotationMirror canonicalAnno = (aliasedAnno != null) ? aliasedAnno : anno;

            if( anno.position.parameter_index != paramIndex ||
                !typeFactory.isSupportedQualifier(canonicalAnno)) {
                continue;
            }

            if( ElementAnnotationUtil.isOnNestedType(anno) ) {
                applyComponentAnnotation(anno);

            } else  if( anno.position.type == upperBoundTarget() ) {
                upperBoundAnnos.add(anno);

            } else {
                lowerBoundAnnos.add(anno);
            }
        }

        applyLowerBounds(lowerBoundAnnos);
        applyUpperBounds(upperBoundAnnos);
    }

    /**
     * Applies a list of annotations to the upperBound of the type parameter.  If the type of
     * the upper bound is an intersection we must first find the correct location for each
     * annotation.
     */
    private void applyUpperBounds( final List<TypeCompound> upperBounds) {
        if(!upperBounds.isEmpty()) {
            final AnnotatedTypeMirror upperBoundType = typeParam.getUpperBound();


            if (upperBoundType.getKind() == TypeKind.INTERSECTION) {

                final List<? extends AnnotatedTypeMirror> intersectionTypes = upperBoundType.directSuperTypes();
                final int boundIndexOffset = getBoundIndexOffset(intersectionTypes);

                for (final TypeCompound anno : upperBounds) {
                    final int boundIndex = anno.position.bound_index + boundIndexOffset;

                    if (boundIndex < 0 || boundIndex > intersectionTypes.size()) {
                        ErrorReporter.errorAbort("Invalid bound index on element annotation ( " + anno + " ) " +
                                "for type ( " + typeParam + " ) with " +
                                "upper bound ( " + typeParam.getUpperBound() + " ) " +
                                "and boundIndex( " + boundIndex + " ) ");
                    }

                    intersectionTypes.get(boundIndex).replaceAnnotation(anno); //TODO: WHY NOT ADD?
                }

            } else {
                upperBoundType.addAnnotations(upperBounds);
            }
        }
    }

    /**
     * In the event of multiple annotations on an AnnotatedNullType lower bound we want to preserve
     * the multiple annotations so that an type.invalid exception is raised later.
     */
    private void applyLowerBounds(final List<? extends AnnotationMirror> annos) {
        if( !annos.isEmpty() ) {
            final AnnotatedTypeMirror lowerBound = typeParam.getLowerBound();

            //Replace only the bottom annotation then use add so that we still end up with an invalid type in
            //the event of multiple annos.  This is to preserve current behavior but we might want to report
            //an error earlier
            lowerBound.replaceAnnotation(annos.get(0));
            for(int i = 1; i < annos.size(); i++) {
                lowerBound.addAnnotation(annos.get(i));
            }
        }
    }

    private void applyComponentAnnotation(final TypeCompound anno) {
        final AnnotatedTypeMirror upperBoundType = typeParam.getUpperBound();

        if( anno.position.type == upperBoundTarget() ) {

            if( upperBoundType.getKind() == TypeKind.INTERSECTION ) {
                final List<? extends AnnotatedTypeMirror> intersectionTypes = upperBoundType.directSuperTypes();
                final int boundIndex = anno.position.bound_index + getBoundIndexOffset(intersectionTypes);

                if(boundIndex < 0 || boundIndex > intersectionTypes.size()) {
                    ErrorReporter.errorAbort("Invalid bound index on element annotation ( " + anno + " ) " +
                            "for type ( " + typeParam + " ) with upper bound ( " + typeParam.getUpperBound() + " )");
                }

                annotateViaTypeAnnoPosition(intersectionTypes.get(boundIndex), anno);
            } else {
                annotateViaTypeAnnoPosition(upperBoundType, anno);

            }

        } else {
            annotateViaTypeAnnoPosition(typeParam.getLowerBound(), anno);
        }
    }
}
