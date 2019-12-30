/**
 * ShapeChange - processing application schemas for geographic information
 *
 * This file is part of ShapeChange. ShapeChange takes a ISO 19109 
 * Application Schema from a UML model and translates it into a 
 * GML Application Schema or other implementation representations.
 *
 * Additional information about the software can be found at
 * http://shapechange.net/
 *
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * interactive instruments GmbH
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Profile;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class ProfileUtil {

	/**
	 * @param model tbd
	 * @param searchInWholeModel
	 *            <code>true</code> if profiles shall be looked up in the whole
	 *            model, <code>false</code> if the lookup shall only be
	 *            performed in the schemas selected for processing.
	 * @return Set with names of profiles defined by elements of the given
	 *         model. Can be empty but not <code>null</code>.
	 */
	public static SortedSet<String> findNamesOfAllProfiles(Model model,
			boolean searchInWholeModel) {

		SortedSet<String> result = new TreeSet<String>();

		if (model != null) {

			Set<PackageInfo> packagesToSearchIn = searchInWholeModel
					? model.packages() : model.allPackagesFromSelectedSchemas();

			for (PackageInfo pkg : packagesToSearchIn) {

				for (ClassInfo ci : pkg.containedClasses()) {

					result.addAll(ci.profiles().getProfileIdentifiersByName()
							.keySet());

					for (PropertyInfo pi : ci.properties().values()) {

						result.addAll(pi.profiles()
								.getProfileIdentifiersByName().keySet());
					}
				}
			}
		}

		return result;
	}

	/**
	 * @param genModel
	 *            The model whose profile definitions are not explicit (i.e.
	 *            classes without profile definitions belong to all profiles,
	 *            and properties without profile definitions inherit the
	 *            profiles of their class). Must not be <code>null</code>.
	 * @param profilesForClassesBelongingToAllProfiles
	 *            Set of profiles that shall be assigned to classes that belong
	 *            to all profiles (i.e., they do not have any profile definition
	 *            in the given model, which is what this method is meant to
	 *            convert into a set of explicit profile definitions). Must not
	 *            be <code>null</code>.
	 * @param schemaNameRegex
	 *            Regular expression to match the name of schemas in which the
	 *            profile definitions shall be converted to explicit ones. If
	 *            this parameter is <code>null</code>, the conversion will be
	 *            applied to all classes and properties of the model if
	 *            parameter 'convertWholeModel' is true, otherwise just of the
	 *            schemas selected for processing.
	 * @param convertWholeModel
	 *            <code>true</code> if conversion shall be applied to all
	 *            classes of the model, <code>false</code> if it shall only be
	 *            applied to the schemas selected for processing; irrelevant if
	 *            parameter 'schemaNameRegex' is not <code>null</code>
	 */
	public static void convertToExplicitProfileDefinitions(
			GenericModel genModel,
			Profiles profilesForClassesBelongingToAllProfiles,
			Pattern schemaNameRegex, boolean convertWholeModel) {

		SortedSet<PackageInfo> selSchemaPkgs = genModel
				.allPackagesFromSelectedSchemas();

		Set<GenericClassInfo> genCisToProcess = new HashSet<GenericClassInfo>();

		for (GenericClassInfo genCi : genModel.getGenClasses().values()) {

			if (schemaNameRegex != null) {

				PackageInfo schemaPkg = genModel.schemaPackage(genCi);

				if (schemaPkg != null && schemaNameRegex
						.matcher(schemaPkg.name()).matches()) {
					genCisToProcess.add(genCi);
				}

			} else {

				if (convertWholeModel || selSchemaPkgs.contains(genCi.pkg())) {
					genCisToProcess.add(genCi);
				}
			}
		}

		for (GenericClassInfo genCi : genCisToProcess) {

			if (genCi.profiles().isEmpty()) {

				genCi.setProfiles(
						profilesForClassesBelongingToAllProfiles.createCopy());
			}

			for (PropertyInfo pi : genCi.properties().values()) {

				if (pi.profiles().isEmpty()) {

					/*
					 * Use a copy of the profiles defined by the class
					 */
					GenericPropertyInfo genPi = (GenericPropertyInfo) pi;
					genPi.setProfiles(genCi.profiles().createCopy());
				}
			}
		}
	}

	/**
	 * Removes the profiles in all classes and properties of the given model.
	 * Also removes the tagged value 'profiles' on these model elements.
	 * 
	 * @param genModel tbd
	 */
	public static void removeProfiles(GenericModel genModel) {

		for (GenericClassInfo genCi : genModel.getGenClasses().values()) {
			genCi.setProfiles(null);
			genCi.removeTaggedValue("profiles");
		}

		for (GenericPropertyInfo genPi : genModel.getGenProperties().values()) {
			genPi.setProfiles(null);
			genPi.removeTaggedValue("profiles");
		}
	}

	/**
	 * Transfers a set of profiles from a source class to a target class. If the
	 * target already contained a profile with the name of a profile that is
	 * transferred, the transferred profile will replace the previously existing
	 * profile.
	 * 
	 * @param namesOfProfilesToTransfer
	 *            Names of profiles to transfer from the source to the target
	 *            class. If <code>null</code>, all profiles shall be
	 *            transferred.
	 * @param sourceCi
	 *            Source of profiles to be transferred
	 * @param targetCi
	 *            Target of the profile transfer
	 */
	public static void transferProfiles(
			SortedSet<String> namesOfProfilesToTransfer, ClassInfo sourceCi,
			ClassInfo targetCi) {

		/*
		 * Determine which profiles shall be transferred.
		 */
		SortedSet<ProfileIdentifier> profilesToTransfer;
		if (namesOfProfilesToTransfer == null) {
			profilesToTransfer = sourceCi.profiles().getProfileIdentifiers();
		} else {
			profilesToTransfer = sourceCi.profiles()
					.getProfiles(namesOfProfilesToTransfer);
		}

		for (ProfileIdentifier profile : profilesToTransfer) {

			/*
			 * NOTE: This will override a previously existing profile with same
			 * name
			 */
			targetCi.profiles().put(profile);
		}
	}

	/**
	 * Transfers a set of profiles from a source property to a target property.
	 * If the target already contained a profile with the name of a profile that
	 * is transferred, the transferred profile will replace the previously
	 * existing profile.
	 * 
	 * @param namesOfProfilesToTransfer
	 *            Names of profiles to transfer from the source to the target
	 *            property. If <code>null</code>, all profiles shall be
	 *            transferred.
	 * @param sourcePi
	 *            Source of profiles to be transferred
	 * @param targetPi
	 *            Target of the profile transfer
	 */
	public static void transferProfiles(
			SortedSet<String> namesOfProfilesToTransfer, PropertyInfo sourcePi,
			PropertyInfo targetPi) {

		/*
		 * Determine which profiles shall be transferred.
		 */
		SortedSet<ProfileIdentifier> profilesToTransfer;
		if (namesOfProfilesToTransfer == null) {
			profilesToTransfer = sourcePi.profiles().getProfileIdentifiers();
		} else {
			profilesToTransfer = sourcePi.profiles()
					.getProfiles(namesOfProfilesToTransfer);
		}

		for (ProfileIdentifier profile : profilesToTransfer) {

			/*
			 * NOTE: This will override a previously existing profile with same
			 * name
			 */
			targetPi.profiles().put(profile);
		}
	}
}
