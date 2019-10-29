package de.interactive_instruments.ShapeChange.Util.ea;

import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;

public class EAGeneralUtil {

	public static Stereotypes createAndPopulateStereotypeCache(
			String eaStereotypeEx, String[] wellKnownStereotypes, Info info) {
		info.result().addDebug(null, 50, info.name(), eaStereotypeEx);
		Stereotypes stereotypesCache = info.options().stereotypesFactory();
		String[] eaStereotypes = eaStereotypeEx.split("\\,");
		for (String stereotype : eaStereotypes) {
			String normalizedStereotype = info.options()
					.normalizeStereotype(stereotype);
			if (normalizedStereotype != null) {
				info.result().addDebug(null, 51, info.name(), stereotype,
						normalizedStereotype);
				boolean wellKnownStereotypeFound = false;
				for (String s : wellKnownStereotypes) {
					if (normalizedStereotype.toLowerCase().equals(s)) {
						stereotypesCache.add(s);
						info.result().addDebug(null, 52, info.name(), s);
						wellKnownStereotypeFound = true;
					}
				}
				if (!wellKnownStereotypeFound) {
					info.result().addDebug(null, 53, info.name(),
							normalizedStereotype);
				}
			}
		}
		info.result().addDebug(null, 54, info.name(),
				Integer.toString(stereotypesCache.size()),
				stereotypesCache.toString());
		return stereotypesCache;
	}

}
