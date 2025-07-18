package io.lionweb.model.impl;

import io.lionweb.language.Annotation;
import io.lionweb.model.AnnotationInstance;
import io.lionweb.model.ClassifierInstance;
import java.util.Objects;

public class DynamicAnnotationInstance extends DynamicClassifierInstance<Annotation>
    implements AnnotationInstance {

  private Annotation annotation;
  private ClassifierInstance<?> annotated;

  public DynamicAnnotationInstance(String id) {
    this.id = id;
  }

  public DynamicAnnotationInstance(String id, Annotation annotation) {
    this(id);
    this.annotation = annotation;
  }

  public DynamicAnnotationInstance(
      String id, Annotation annotation, ClassifierInstance<?> annotated) {
    this(id, annotation);
    setAnnotated(annotated);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DynamicAnnotationInstance)) {
      return false;
    }
    DynamicAnnotationInstance that = (DynamicAnnotationInstance) o;
    return Objects.equals(annotation, that.annotation)
        && Objects.equals(getID(), that.getID())
        && Objects.equals(annotated, that.annotated)
        && Objects.equals(propertyValues, that.propertyValues)
        && Objects.equals(
            containmentValues == null || containmentValues.isEmpty() ? null : containmentValues,
            that.containmentValues == null || that.containmentValues.isEmpty()
                ? null
                : that.containmentValues)
        && Objects.equals(
            referenceValues == null || referenceValues.isEmpty() ? null : referenceValues,
            that.referenceValues == null || that.referenceValues.isEmpty()
                ? null
                : that.referenceValues)
        && Objects.equals(
            annotations == null || annotations.isEmpty() ? null : annotations,
            that.annotations == null || that.annotations.isEmpty() ? null : that.annotations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getID(), annotation, annotated);
  }

  public void setAnnotation(Annotation annotation) {
    this.annotation = annotation;
  }

  public void setAnnotated(ClassifierInstance<?> annotated) {
    if (annotated == this.annotated) {
      // necessary to avoid infinite loops
      return;
    }
    if (this.annotated != null && this.annotated instanceof DynamicNode) {
      ((DynamicNode) this.annotated).tryToRemoveAnnotation(this);
    }
    this.annotated = annotated;
    if (this.annotated != null && this.annotated instanceof AbstractClassifierInstance) {
      ((AbstractClassifierInstance<?>) this.annotated).addAnnotation(this);
    }
  }

  @Override
  public Annotation getAnnotationDefinition() {
    return annotation;
  }

  @Override
  public ClassifierInstance<?> getParent() {
    return annotated;
  }

  @Override
  public String toString() {
    String annotatedDesc = null;
    if (annotated != null) {
      annotatedDesc = annotated.getID();
    }
    return "DynamicAnnotationInstance{"
        + "annotation="
        + annotation
        + ", annotated="
        + annotatedDesc
        + '}';
  }
}
