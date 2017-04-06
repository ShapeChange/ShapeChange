package de.interactive_instruments.ShapeChange.Model;

public enum Descriptor {

	ALIAS("alias", true), PRIMARYCODE("primaryCode", true), DOCUMENTATION(
			"documentation", true), DEFINITION("definition", true), DESCRIPTION(
					"description", true), EXAMPLE("example", false), LEGALBASIS(
							"legalBasis", true), DATACAPTURESTATEMENT(
									"dataCaptureStatement", false), LANGUAGE(
											"language", true), GLOBALIDENTIFIER(
													"globalIdentifier", true);

	private String name = null;
	private boolean singleValued = false;

	Descriptor(String n, boolean sv) {
		name = n;
		singleValued = sv;
	}

	public boolean isSingleValued() {
		return singleValued;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return name;
	}
}