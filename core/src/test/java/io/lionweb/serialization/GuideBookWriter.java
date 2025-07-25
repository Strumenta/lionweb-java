package io.lionweb.serialization;

import io.lionweb.language.Concept;

public class GuideBookWriter extends Writer {
  public GuideBookWriter(String id, String name) {
    super(id, name, LibraryLanguage.GUIDE_BOOK_WRITER);
  }

  public void setCountries(String countries) {
    setPropertyValue(getClassifier().getPropertyByName("countries"), countries);
  }

  @Override
  public Concept getClassifier() {
    return LibraryLanguage.GUIDE_BOOK_WRITER;
  }
}
