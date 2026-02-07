/*
 * This file is part of JPARSEC library.
 *
 * (C) Copyright 2006-2020 by T. Alonso Albi - OAN (Spain).
 *
 * Project Info:  http://conga.oan.es/~alonso/jparsec/jparsec.html
 *
 * JPARSEC library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JPARSEC library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package jparsec.observer;

import jparsec.observer.ObserverElement.DST_RULE;
import jparsec.time.TimeScale;
import jparsec.util.*;

/**
 * Convenient class for country selection.
 * <P>
 * This class provides constants ID for all the countries available for
 * selection, including information for correcting the daylight saving time in
 * each one. The list of countries includes all existing ones updated to 2011,
 * except a few of them with very little population. Also, some of the
 * 'countries' are (speaking rigorously) just regions, like 'Antarctica' or
 * 'Terres Australes'.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Country
{
	// private constructor so that this class cannot be instantiated.
	private Country() {}

	/**
	 * The set of countries.
	 */
	public enum COUNTRY {
		/** Constant ID for Afghanistan. */
		Afghanistan,
		/** Constant ID for Albania. */
		Albania,
		/** Constant ID for Algeria. */
		Algeria,
		/** Constant ID for American Samoa. */
		American_Samoa,
		/** Constant ID for Andorra. */
		Andorra,
		/** Constant ID for Angola. */
		Angola,
		/** Constant ID for Anguilla. */
		Anguilla,
		/** Constant ID for Antarctica. */
		Antarctica,
		/** Constant ID for Antigua &amp; Barbuda. */
		Antigua_and_Barbuda,
		/** Constant ID for Argentina. */
		Argentina,
		/** Constant ID for Armenia. */
		Armenia,
		/** Constant ID for Aruba. */
		Aruba,
		/** Constant ID for Australia. */
		Australia,
		/** Constant ID for Austria. */
		Austria,
		/** Constant ID for Azerbaijan. */
		Azerbaijan,
		/** Constant ID for Bahamas. */
		Bahamas,
		/** Constant ID for Bahrain. */
		Bahrain,
		/** Constant ID for Bangladesh. */
		Bangladesh,
		/** Constant ID for Barbados. */
		Barbados,
		/** Constant ID for Belarus. */
		Belarus,
		/** Constant ID for Belgium. */
		Belgium,
		/** Constant ID for Belize. */
		Belize,
		/** Constant ID for Benin. */
		Benin,
		/** Constant ID for Bermuda. */
		Bermuda,
		/** Constant ID for Bhutan. */
		Bhutan,
		/** Constant ID for Bolivia. */
		Bolivia,
		/** Constant ID for Bosnia-Herzegovina. */
		Bosnia_Herzegovina,
		/** Constant ID for Botswana. */
		Botswana,
		/** Constant ID for Brazil. */
		Brazil,
		/** Constant ID for Brunei. */
		Brunei,
		/** Constant ID for Bulgaria. */
		Bulgaria,
		/** Constant ID for Burkina Faso. */
		Burkina_Faso,
		/** Constant ID for Burundi. */
		Burundi,
		/** Constant ID for Cambodia. */
		Cambodia,
		/** Constant ID for Cameroon. */
		Cameroon,
		/** Constant ID for Canada. */
		Canada,
		/** Constant ID for Cape Verde. */
		Cape_Verde,
		/** Constant ID for Cayman Islands. */
		Cayman_Islands,
		/** Constant ID for Central Africa. */
		Central_Africa,
		/** Constant ID for Chad. */
		Chad,
		/** Constant ID for Chile. */
		Chile,
		/** Constant ID for China. */
		China,
		/** Constant ID for Colombia. */
		Colombia,
		/** Constant ID for Comoros. */
		Comoros,
		/** Constant ID for Congo. */
		Congo,
		/** Constant ID for Congo (Dem. Rep.). */
		Congo_Dem_Rep,
		/** Constant ID for Cook Islands. */
		Cook_Islands,
		/** Constant ID for Costa Rica. */
		Costa_Rica,
		/** Constant ID for Croatia. */
		Croatia,
		/** Constant ID for Cuba. */
		Cuba,
		/** Constant ID for Cyprus. */
		Cyprus,
		/** Constant ID for Czech Republic. */
		Czech_Republic,
		/** Constant ID for Denmark. */
		Denmark,
		/** Constant ID for Djibouti. */
		Djibouti,
		/** Constant ID for Dominica. */
		Dominica,
		/** Constant ID for Dominican Republic. */
		Dominican_Republic,
		/** Constant ID for East Timor. */
		East_Timor,
		/** Constant ID for Ecuador. */
		Ecuador,
		/** Constant ID for Egypt. */
		Egypt,
		/** Constant ID for El Salvador. */
		El_Salvador,
		/** Constant ID for Equatorial Guinea. */
		Equatorial_Guinea,
		/** Constant ID for Eritrea. */
		Eritrea,
		/** Constant ID for Estonia. */
		Estonia,
		/** Constant ID for Ethiopia. */
		Ethiopia,
		/** Constant ID for Falkland Islands and dependencies. */
		Falkland_Islands_and_dependencies,
		/** Constant ID for Faroe Islands. */
		Faroe_Islands,
		/** Constant ID for Fiji. */
		Fiji,
		/** Constant ID for Finland. */
		Finland,
		/** Constant ID for France. */
		France,
		/** Constant ID for Guadeloupe archipelago (France). */
		French_Guadeloupe,
		/** Constant ID for French Guiana. */
		French_Guiana,
		/** Constant ID for Martinique island (France). */
		French_Martinique,
		/** Constant ID for French Polynesia. */
		French_Polynesia,
		/** Constant ID for Gabon. */
		Gabon,
		/** Constant ID for Gambia. */
		Gambia,
		/** Constant ID for Georgia. */
		Georgia,
		/** Constant ID for Germany. */
		Germany,
		/** Constant ID for Ghana. */
		Ghana,
		/** Constant ID for Gibraltar. */
		Gibraltar,
		/** Constant ID for Greece. */
		Greece,
		/** Constant ID for Greenland. */
		Greenland,
		/** Constant ID for Grenada. */
		Grenada,
		/** Constant ID for Guam. */
		Guam,
		/** Constant ID for Guatemala. */
		Guatemala,
		/** Constant ID for Guernsey and Alderney. */
		Guernsey_and_Alderney,
		/** Constant ID for Guinea. */
		Guinea,
		/** Constant ID for Guinea-Bissau. */
		Guinea_Bissau,
		/** Constant ID for Guyana. */
		Guyana,
		/** Constant ID for Haiti. */
		Haiti,
		/** Constant ID for Honduras. */
		Honduras,
		/** Constant ID for Hungary. */
		Hungary,
		/** Constant ID for Iceland. */
		Iceland,
		/** Constant ID for India. */
		India,
		/** Constant ID for Indonesia. */
		Indonesia,
		/** Constant ID for Iran. */
		Iran,
		/** Constant ID for Iraq. */
		Iraq,
		/** Constant ID for Ireland. */
		Ireland,
		/** Constant ID for Israel. */
		Israel,
		/** Constant ID for Italy. */
		Italy,
		/** Constant ID for Ivory Coast. */
		Ivory_Coast,
		/** Constant ID for Jamaica. */
		Jamaica,
		/** Constant ID for Japan. */
		Japan,
		/** Constant ID for Jersey. */
		Jersey,
		/** Constant ID for Jordan. */
		Jordan,
		/** Constant ID for Kazakhstan. */
		Kazakhstan,
		/** Constant ID for Kenya. */
		Kenya,
		/** Constant ID for Kiribati. */
		Kiribati,
		/** Constant ID for Korea of North. */
		Korea_of_North,
		/** Constant ID for Korea of South. */
		Korea_of_South,
		/** Constant ID for Kuwait. */
		Kuwait,
		/** Constant ID for Kyrgyzstan. */
		Kyrgyzstan,
		/** Constant ID for Laos. */
		Laos,
		/** Constant ID for Latvia. */
		Latvia,
		/** Constant ID for Lebanon. */
		Lebanon,
		/** Constant ID for Lesotho. */
		Lesotho,
		/** Constant ID for Liberia. */
		Liberia,
		/** Constant ID for Libya. */
		Libya,
		/** Constant ID for Liechtenstein. */
		Liechtenstein,
		/** Constant ID for Lithuania. */
		Lithuania,
		/** Constant ID for Luxembourg. */
		Luxembourg,
		/** Constant ID for Macedonia. */
		Macedonia,
		/** Constant ID for Madagascar. */
		Madagascar,
		/** Constant ID for Malawi. */
		Malawi,
		/** Constant ID for Malaysia. */
		Malaysia,
		/** Constant ID for Maldives. */
		Maldives,
		/** Constant ID for Mali. */
		Mali,
		/** Constant ID for Malta. */
		Malta,
		/** Constant ID for Man (Isle of). */
		Man_Isle_of,
		/** Constant ID for Marshall Islands. */
		Marshall_Islands,
		/** Constant ID for Mauritania. */
		Mauritania,
		/** Constant ID for Mauritius. */
		Mauritius,
		/** Constant ID for Mayotte. */
		Mayotte,
		/** Constant ID for Mexico. */
		Mexico,
		/** Constant ID for Micronesia. */
		Micronesia,
		/** Constant ID for Moldova. */
		Moldova,
		/** Constant ID for Monaco. */
		Monaco,
		/** Constant ID for Mongolia. */
		Mongolia,
		/** Constant ID for Montenegro. */
		Montenegro,
		/** Constant ID for Montserrat. */
		Montserrat,
		/** Constant ID for Morocco. */
		Morocco,
		/** Constant ID for Mozambique. */
		Mozambique,
		/** Constant ID for Myanmar. */
		Myanmar,
		/** Constant ID for Namibia. */
		Namibia,
		/** Constant ID for Nauru. */
		Nauru,
		/** Constant ID for Nepal. */
		Nepal,
		/** Constant ID for Netherlands. */
		Netherlands,
		/** Constant ID for Netherlands Antilles. */
		Netherlands_Antilles,
		/** Constant ID for New Caledonia. */
		New_Caledonia,
		/** Constant ID for New Zealand. */
		New_Zealand,
		/** Constant ID for Nicaragua. */
		Nicaragua,
		/** Constant ID for Niger. */
		Niger,
		/** Constant ID for Nigeria. */
		Nigeria,
		/** Constant ID for Niue. */
		Niue,
		/** Constant ID for Norfolk Island. */
		Norfolk_Island,
		/** Constant ID for Northern Mariana Islands. */
		Northern_Mariana_Islands,
		/** Constant ID for Norway. */
		Norway,
		/** Constant ID for Oman. */
		Oman,
		/** Constant ID for Pakistan. */
		Pakistan,
		/** Constant ID for Palau. */
		Palau,
		/** Constant ID for Palestine. */
		Palestine,
		/** Constant ID for Panama. */
		Panama,
		/** Constant ID for Papua New Guinea. */
		Papua_New_Guinea,
		/** Constant ID for Paraguay. */
		Paraguay,
		/** Constant ID for Peru. */
		Peru,
		/** Constant ID for Philippines. */
		Philippines,
		/** Constant ID for Poland. */
		Poland,
		/** Constant ID for Portugal. */
		Portugal,
		/** Constant ID for Puerto Rico. */
		Puerto_Rico,
		/** Constant ID for Qatar. */
		Qatar,
		/** Constant ID for R&eacute;union. */
		Reunion,
		/** Constant ID for Romania. */
		Romania,
		/** Constant ID for Russia. */
		Russia,
		/** Constant ID for Rwanda. */
		Rwanda,
		/** Constant ID for Sahara. */
		Sahara,
		/** Constant ID for Saint Helena. */
		Saint_Helena,
		/** Constant ID for Saint Kitts and Nevis. */
		Saint_Kitts_and_Nevis,
		/** Constant ID for Saint Lucia. */
		Saint_Lucia,
		/** Constant ID for Saint Pierre &amp; Miquelon. */
		Saint_Pierre_and_Miquelon,
		/** Constant ID for Saint Vincent and the Grenadines. */
		Saint_Vincent_and_the_Grenadines,
		/** Constant ID for Samoa. */
		Samoa,
		/** Constant ID for San Marino. */
		San_Marino,
		/** Constant ID for S&atilde;o Tom&eacute; and Pr&iacute;ncipe. */
		Sao_Tome_and_Principe,
		/** Constant ID for Saudi Arabia. */
		Saudi_Arabia,
		/** Constant ID for Senegal. */
		Senegal,
		/** Constant ID for Serbia. */
		Serbia,
		/** Constant ID for Seychelles. */
		Seychelles,
		/** Constant ID for Sierra Leone. */
		Sierra_Leone,
		/** Constant ID for Singapore. */
		Singapore,
		/** Constant ID for Slovakia. */
		Slovakia,
		/** Constant ID for Slovenia. */
		Slovenia,
		/** Constant ID for Solomon Islands. */
		Solomon_Islands,
		/** Constant ID for Somalia. */
		Somalia,
		/** Constant ID for South Africa Republic. */
		South_Africa_Republic,
		/** Constant ID for Spain. */
		Spain,
		/** Constant ID for Sri Lanka. */
		Sri_Lanka,
		/** Constant ID for Sudan. */
		Sudan,
		/** Constant ID for Suriname. */
		Suriname,
		/** Constant ID for Svalbard and Jan Mayen. */
		Svalbard_and_Jan_Mayen,
		/** Constant ID for Swaziland. */
		Swaziland,
		/** Constant ID for Sweden. */
		Sweden,
		/** Constant ID for Switzerland. */
		Switzerland,
		/** Constant ID for Syria. */
		Syria,
		/** Constant ID for Taiwan. */
		Taiwan,
		/** Constant ID for Tajikistan. */
		Tajikistan,
		/** Constant ID for Tanzania. */
		Tanzania,
		/** Constant ID for Terres Australes. */
		Terres_Australes,
		/** Constant ID for Thailand. */
		Thailand,
		/** Constant ID for Togo. */
		Togo,
		/** Constant ID for Tokelau. */
		Tokelau,
		/** Constant ID for Tonga. */
		Tonga,
		/** Constant ID for Trinidad and Tobago. */
		Trinidad_and_Tobago,
		/** Constant ID for Tunisia. */
		Tunisia,
		/** Constant ID for Turkey. */
		Turkey,
		/** Constant ID for Turkmenistan. */
		Turkmenistan,
		/** Constant ID for Turks and Caicos Islands. */
		Turks_and_Caicos_Islands,
		/** Constant ID for Tuvalu. */
		Tuvalu,
		/** Constant ID for Uganda. */
		Uganda,
		/** Constant ID for Ukraine. */
		Ukraine,
		/** Constant ID for United Arab Emirates. */
		United_Arab_Emirates,
		/** Constant ID for United Kingdom. */
		United_Kingdom,
		/** Constant ID for United States of America. */
		United_States_of_America,
		/** Constant ID for Uruguay. */
		Uruguay,
		/** Constant ID for Uzbekistan. */
		Uzbekistan,
		/** Constant ID for Vanuatu. */
		Vanuatu,
		/** Constant ID for Vatican. */
		Vatican,
		/** Constant ID for Venezuela. */
		Venezuela,
		/** Constant ID for Vietnam. */
		Vietnam,
		/** Constant ID for Virgin Islands of the UK. */
		Virgin_Islands_of_the_UK,
		/** Constant ID for Virgin Islands of the United States. */
		Virgin_Islands_of_the_United_States,
		/** Constant ID for Wallis &amp; Futuna. */
		Wallis_and_Futuna,
		/** Constant ID for Yemen. */
		Yemen,
		/** Constant ID for Zambia. */
		Zambia,
		/** Constant ID for Sudan. */
		SudanOfSouth,
		/** Constant ID for Zimbabwe. */
		Zimbabwe	;

		/**
		 * Gets the name of the country providing the ID constant.
		 *
		 * @return The country name.
		 */
		@Override
		public String toString()
		{
			if (this == COUNTRY.American_Samoa)
				return "American Samoa";

			if (this == COUNTRY.Antigua_and_Barbuda)
				return "Antigua & Barbuda";

			if (this == COUNTRY.Bosnia_Herzegovina)
				return "Bosnia-Herzegovina";

			if (this == COUNTRY.Burkina_Faso)
				return "Burkina Faso";

			if (this == COUNTRY.Cape_Verde)
				return "Cape Verde";

			if (this == COUNTRY.Cayman_Islands)
				return "Cayman Islands";

			if (this == COUNTRY.Central_Africa)
				return "Central Africa";

			if (this == COUNTRY.Congo_Dem_Rep)
				return "Congo (Dem. Rep.)";

			if (this == COUNTRY.Cook_Islands)
				return "Cook Islands";

			if (this == COUNTRY.Costa_Rica)
				return "Costa Rica";

			if (this == COUNTRY.Czech_Republic)
				return "Czech Republic";

			if (this == COUNTRY.Dominican_Republic)
				return "Dominican Republic";

			if (this == COUNTRY.East_Timor)
				return "East Timor";

			if (this == COUNTRY.El_Salvador)
				return "El Salvador";

			if (this == COUNTRY.Equatorial_Guinea)
				return "Equatorial Guinea";

			if (this == COUNTRY.Falkland_Islands_and_dependencies)
				return "Falkland Islands and dependencies";

			if (this == COUNTRY.Faroe_Islands)
				return "Faroe Islands";

			if (this == COUNTRY.French_Guiana)
				return "French Guiana";

			if (this == COUNTRY.French_Polynesia)
				return "French Polynesia";

			if (this == COUNTRY.French_Guadeloupe)
				return "Guadeloupe";

			if (this == COUNTRY.Guernsey_and_Alderney)
				return "Guernsey and Alderney";

			if (this == COUNTRY.Guinea_Bissau)
				return "Guinea-Bissau";

			if (this == COUNTRY.Ivory_Coast)
				return "Ivory Coast";

			if (this == COUNTRY.Korea_of_North)
				return "Korea of North";

			if (this == COUNTRY.Korea_of_South)
				return "Korea of South";

			if (this == COUNTRY.Man_Isle_of)
				return "Man (Isle of)";

			if (this == COUNTRY.Marshall_Islands)
				return "Marshall Islands";

			if (this == COUNTRY.French_Martinique)
				return "Martinique";

			if (this == COUNTRY.Netherlands_Antilles)
				return "Netherlands Antilles";

			if (this == COUNTRY.New_Caledonia)
				return "New Caledonia";

			if (this == COUNTRY.New_Zealand)
				return "New Zealand";

			if (this == COUNTRY.Norfolk_Island)
				return "Norfolk Island";

			if (this == COUNTRY.Northern_Mariana_Islands)
				return "Northern Mariana Islands";

			if (this == COUNTRY.Papua_New_Guinea)
				return "Papua New Guinea";

			if (this == COUNTRY.Puerto_Rico)
				return "Puerto Rico";

			if (this == COUNTRY.Reunion)
				return "R\u00e9union";

			if (this == COUNTRY.Saint_Helena)
				return "Saint Helena";

			if (this == COUNTRY.Saint_Kitts_and_Nevis)
				return "Saint Kitts and Nevis";

			if (this == COUNTRY.Saint_Lucia)
				return "Saint Lucia";

			if (this == COUNTRY.Saint_Pierre_and_Miquelon)
				return "Saint Pierre & Miquelon";

			if (this == COUNTRY.Saint_Vincent_and_the_Grenadines)
				return "Saint Vincent and the Grenadines";

			if (this == COUNTRY.San_Marino)
				return "San Marino";

			if (this == COUNTRY.Sao_Tome_and_Principe)
				return "S\u00e3o Tom\u00e9 and Pr\u00edncipe";

			if (this == COUNTRY.Saudi_Arabia)
				return "Saudi Arabia";

			if (this == COUNTRY.Sierra_Leone)
				return "Sierra Leone";

			if (this == COUNTRY.Solomon_Islands)
				return "Solomon Islands";

			if (this == COUNTRY.South_Africa_Republic)
				return "South Africa Republic";

			if (this == COUNTRY.Sri_Lanka)
				return "Sri Lanka";

			if (this == COUNTRY.SudanOfSouth)
				return "Sudan of South";

			if (this == COUNTRY.Svalbard_and_Jan_Mayen)
				return "Svalbard and Jan Mayen";

			if (this == COUNTRY.Terres_Australes)
				return "Terres Australes";

			if (this == COUNTRY.Trinidad_and_Tobago)
				return "Trinidad and Tobago";

			if (this == COUNTRY.Turks_and_Caicos_Islands)
				return "Turks and Caicos Islands";

			if (this == COUNTRY.United_Arab_Emirates)
				return "United Arab Emirates";

			if (this == COUNTRY.United_Kingdom)
				return "United Kingdom";

			if (this == COUNTRY.United_States_of_America)
				return "United States of America";

			if (this == COUNTRY.Virgin_Islands_of_the_UK)
				return "Virgin Islands of the UK";

			if (this == COUNTRY.Virgin_Islands_of_the_United_States)
				return "Virgin Islands of the United States";

			if (this == COUNTRY.Wallis_and_Futuna)
				return "Wallis & Futuna";


			return this.name();
		}

		/**
		 * Returns the daylight saving time rule for a given country.
		 * <P>
		 * Values have been stablished using Wikipedia as source, but this method
		 * does not provide valid output for all countries. For Europe is fine.
		 * <P>
		 * The change is 1 hour and is set at the beginning of the last Sunday in
		 * the corresponding month. See {@link TimeScale#getDST(double, ObserverElement)}
		 * for more information. The only exception to this rule is currently
		 * United States and Canada, where the change starts now on the second
		 * Monday of April, and ends on the first Sunday of November.
		 * <P>
		 * The JPARSEC package applies this change in the same way in the whole USA
		 * (new rule adopted in 2007, old rule previously for the entire country), although
		 * some regions in USA have maintained the old rule. To select the correct rule
		 * it is posible to set DST information properly in the observer object.
		 *
		 * @return The DST code for the country, could be {@linkplain DST_RULE#NONE} if no
		 * DST information exists.
		 * @throws JPARSECException Thrown if DST is unknown and
		 *         {@link JPARSECException#treatWarningsAsErrors(boolean)} is set to true.
		 * @throws JPARSECException In case the no DST information exists and warnings are
		 * configured to launch errors.
		 */
		public DST_RULE getDSTCode() throws JPARSECException
		{
			if (this == COUNTRY.Albania)
				return DST_RULE.N1;

			if (this == COUNTRY.Andorra)
				return DST_RULE.N1;

			if (this == COUNTRY.Australia)
				return DST_RULE.S1;

			if (this == COUNTRY.Austria)
				return DST_RULE.N1;

			//if (this == COUNTRY.Azerbaijan) // No longer after 2015
			//	return DST_RULE.N1;

			if (this == COUNTRY.Bahamas)
				return DST_RULE.N1;

			if (this == COUNTRY.Belgium)
				return DST_RULE.N1;

			if (this == COUNTRY.Bermuda)
				return DST_RULE.N1;

			if (this == COUNTRY.Bosnia_Herzegovina)
				return DST_RULE.N1;

			//if (this == COUNTRY.Brazil) // No longer after 2019
			//	return DST_RULE.S1;

			if (this == COUNTRY.Bulgaria)
				return DST_RULE.N1;

			if (this == COUNTRY.Canada)
				return DST_RULE.USA_AUTO;

			if (this == COUNTRY.Chile)
				return DST_RULE.S1;

			if (this == COUNTRY.Croatia)
				return DST_RULE.N1;

			if (this == COUNTRY.Cuba)
				return DST_RULE.N1;

			if (this == COUNTRY.Czech_Republic)
				return DST_RULE.N1;

			if (this == COUNTRY.Denmark)
				return DST_RULE.N1;

			if (this == COUNTRY.Faroe_Islands)
				return DST_RULE.N1;

			if (this == COUNTRY.Fiji)
				return DST_RULE.N1;

			if (this == COUNTRY.Finland)
				return DST_RULE.N1;

			if (this == COUNTRY.France)
				return DST_RULE.N1;

			//if (this == COUNTRY.French_Guiana)
			//	return DST_RULE.N1;

			//if (this == COUNTRY.Georgia)
			//	return DST_RULE.N1;

			if (this == COUNTRY.Germany)
				return DST_RULE.N1;

			if (this == COUNTRY.Gibraltar)
				return DST_RULE.N1;

			if (this == COUNTRY.Greece)
				return DST_RULE.N1;

			if (this == COUNTRY.Hungary)
				return DST_RULE.N1;

			if (this == COUNTRY.Iran)
				return DST_RULE.N1;

			//if (this == COUNTRY.Iraq)
			//	return DST_RULE.N1;

			if (this == COUNTRY.Ireland)
				return DST_RULE.N1;

			if (this == COUNTRY.Italy)
				return DST_RULE.N1;

			//if (this == COUNTRY.Kazakhstan)
			//	return DST_RULE.N1;

			//if (this == COUNTRY.Kyrgyzstan)
			//	return DST_RULE.N1;

			if (this == COUNTRY.Liechtenstein)
				return DST_RULE.N1;

			if (this == COUNTRY.Luxembourg)
				return DST_RULE.N1;

			if (this == COUNTRY.Macedonia)
				return DST_RULE.N1;

			//if (this == COUNTRY.Maldives)
			//	return DST_RULE.S1;

			if (this == COUNTRY.Malta)
				return DST_RULE.N1;

			if (this == COUNTRY.Mexico)
				return DST_RULE.N1;

			if (this == COUNTRY.Moldova)
				return DST_RULE.N1;

			if (this == COUNTRY.Monaco)
				return DST_RULE.N1;

			//if (this == COUNTRY.Mongolia)
			//	return DST_RULE.N1;

			if (this == COUNTRY.Montenegro)
				return DST_RULE.N1;

			//if (this == COUNTRY.Namibia)
			//	return DST_RULE.N1;

			if (this == COUNTRY.New_Zealand)
				return DST_RULE.S1;

			if (this == COUNTRY.Norway)
				return DST_RULE.N1;

			if (this == COUNTRY.Paraguay)
				return DST_RULE.S1;

			if (this == COUNTRY.Portugal)
				return DST_RULE.N1;

			//if (this == COUNTRY.Puerto_Rico)
			//	return DST_RULE.N1;

			if (this == COUNTRY.Romania)
				return DST_RULE.N1;

			//if (this == COUNTRY.Russia)
			//	return DST_RULE.N1;

			if (this == COUNTRY.Saint_Pierre_and_Miquelon)
				return DST_RULE.N1;

			if (this == COUNTRY.San_Marino)
				return DST_RULE.N1;

			if (this == COUNTRY.Serbia)
				return DST_RULE.N1;

			if (this == COUNTRY.Slovakia)
				return DST_RULE.N1;

			if (this == COUNTRY.Slovenia)
				return DST_RULE.N1;

			if (this == COUNTRY.Spain)
				return DST_RULE.N1;

			//if (this == COUNTRY.Svalbard_and_Jan_Mayen)
			//	return DST_RULE.N1;

			if (this == COUNTRY.Sweden)
				return DST_RULE.N1;

			if (this == COUNTRY.Switzerland)
				return DST_RULE.N1;

			//if (this == COUNTRY.Turkey)
			//	return DST_RULE.N1;

			if (this == COUNTRY.Turks_and_Caicos_Islands)
				return DST_RULE.N1;

			if (this == COUNTRY.Ukraine)
				return DST_RULE.N1;

			if (this == COUNTRY.United_Kingdom)
				return DST_RULE.N1;

			if (this == COUNTRY.United_States_of_America)
				return DST_RULE.USA_AUTO;

			//if (this == COUNTRY.Uruguay)
			//	return DST_RULE.S1;

			// Countries that no longer change the time during the year
			if (this == COUNTRY.Colombia || this == COUNTRY.Armenia ||
					this == COUNTRY.Barbados || this == Belarus || this == Belize ||
					this == COUNTRY.Botswana || this == Brazil || this == COUNTRY.Cape_Verde ||
					this == Chad || this == COUNTRY.Cook_Islands || this == COUNTRY.Costa_Rica || 
					this == Dominican_Republic || this == Ecuador || this == COUNTRY.Egypt || 
					this == COUNTRY.El_Salvador || this == COUNTRY.Falkland_Islands_and_dependencies || 
					this == Georgia || this == Ghana || this == COUNTRY.Guam || this == COUNTRY.Guatemala ||
					this == COUNTRY.Honduras || this == COUNTRY.Iceland || this == India ||
					this == Argentina || this == Bolivia || this == COUNTRY.Iraq || 
					this == China || this == COUNTRY.Mauritius || this == COUNTRY.Algeria  || this == Azerbaijan ||
					this == COUNTRY.Jamaica || this == COUNTRY.Japan || this == Kazakhstan || 
					this == COUNTRY.Kyrgyzstan || this == Lesotho || this == Libya || this == Malaysia || 
					this == COUNTRY.Madagascar || this == Mongolia || this == COUNTRY.Namibia ||
					this == New_Caledonia || this == Nicaragua || this == Pakistan || this == Peru || 
					this == COUNTRY.Philippines || this == COUNTRY.Puerto_Rico || this == COUNTRY.Russia || 
					this == COUNTRY.Singapore || this == Sierra_Leone || this == COUNTRY.South_Africa_Republic || 
					this == Korea_of_South || this == COUNTRY.SudanOfSouth || this == Sri_Lanka || 
					this == COUNTRY.Taiwan || this == COUNTRY.Tajikistan || this == Tonga || this == Tunisia || 
					this == Turkey || this == Turkmenistan || this == COUNTRY.Uruguay || this == COUNTRY.Uzbekistan || 
					this == COUNTRY.Vanuatu ||
					
					// DST never used
					this == COUNTRY.Angola || this == Anguilla || this == COUNTRY.Antigua_and_Barbuda ||
					this == Afghanistan || this == American_Samoa ||   
					this == Bahrain || this == Benin || this == Aruba || 
					this == Bhutan || this == COUNTRY.Virgin_Islands_of_the_UK || 
					this == Brunei || this == COUNTRY.Burkina_Faso || this == COUNTRY.Burundi || 
					this == COUNTRY.Cambodia || this == Cameroon || this == COUNTRY.Cayman_Islands || 
					this == COUNTRY.Comoros || 
					this == Congo || this == Congo_Dem_Rep || this == COUNTRY.Ivory_Coast || 
					this == COUNTRY.Djibouti || this == East_Timor || this == Equatorial_Guinea || 
					this == COUNTRY.Eritrea || this == COUNTRY.Ethiopia || this == COUNTRY.French_Guiana || 
					this == French_Polynesia || this == Gabon || this == COUNTRY.Gambia || 
					this == French_Guadeloupe || this == French_Martinique || 
					this == Guyana || this == Guinea || this == COUNTRY.Guinea_Bissau || 
					this == COUNTRY.Indonesia || this == Kenya || this == COUNTRY.Kiribati || 
					this == COUNTRY.Kuwait || this == Laos || this == Liberia || this == Malawi || 
					this == Maldives || this == Mali || this == COUNTRY.Marshall_Islands || 
					this == COUNTRY.Mauritania || this == COUNTRY.Mayotte || this == Micronesia || 
					this == COUNTRY.Montserrat || this == Mozambique || this == Myanmar || 
					this == COUNTRY.Nauru || this == Nepal || this == Niger || this == COUNTRY.Nigeria || 
					this == Korea_of_North || this == Northern_Mariana_Islands || this == Oman || 
					this == Palau || this == COUNTRY.Panama || this == Papua_New_Guinea || 
					this == Qatar || this == COUNTRY.Reunion || this == COUNTRY.Rwanda || 
					this == Saint_Helena || this == Saint_Lucia || this == Saint_Vincent_and_the_Grenadines || 
					this == COUNTRY.Saint_Kitts_and_Nevis || this == Senegal || this == Saudi_Arabia || 
					this == COUNTRY.Sao_Tome_and_Principe || this == COUNTRY.Seychelles || 
					this == COUNTRY.Somalia || this == Solomon_Islands || 
					this == Svalbard_and_Jan_Mayen || this == COUNTRY.Suriname || 
					this == Tanzania || this == Thailand || this == Togo || 
					this == Tokelau || this == Trinidad_and_Tobago || this == Tuvalu || this == Uganda || 
					this == United_Arab_Emirates || this == Virgin_Islands_of_the_United_States || 
					this == COUNTRY.Vietnam || this == Wallis_and_Futuna || this == COUNTRY.Venezuela || 
					this == Yemen || this == Zambia || this == COUNTRY.Zimbabwe)
				return DST_RULE.NONE;

			JPARSECException
					.addWarning(Translate.translate(274)+" " + this.name()+".");
			return DST_RULE.NONE;
		}
	};

	/**
	 * Returns the number of countries.
	 *
	 * @return The number of countries.
	 */
	public static int getNumberOfCountries()
	{
		return COUNTRY.values().length;
	}


	/**
	 * Gets the ID number of the country providing it's name.
	 *
	 * @param country The name of the country.
	 * @return The ID number.
	 * @throws JPARSECException Thrown if the country is not found.
	 */
	public static COUNTRY getID(String country) throws JPARSECException
	{
		COUNTRY c = null;

		if (country.equals("Afghanistan"))
			c = COUNTRY.Afghanistan;

		if (country.equals("Albania"))
			c = COUNTRY.Albania;

		if (country.equals("Algeria"))
			c = COUNTRY.Algeria;

		if (country.equals("American Samoa"))
			c = COUNTRY.American_Samoa;

		if (country.equals("Andorra"))
			c = COUNTRY.Andorra;

		if (country.equals("Angola"))
			c = COUNTRY.Angola;

		if (country.equals("Anguilla"))
			c = COUNTRY.Anguilla;

		if (country.equals("Antarctica"))
			c = COUNTRY.Antarctica;

		if (country.equals("Antigua & Barbuda"))
			c = COUNTRY.Antigua_and_Barbuda;

		if (country.equals("Argentina"))
			c = COUNTRY.Argentina;

		if (country.equals("Armenia"))
			c = COUNTRY.Armenia;

		if (country.equals("Aruba"))
			c = COUNTRY.Aruba;

		if (country.equals("Australia"))
			c = COUNTRY.Australia;

		if (country.equals("Austria"))
			c = COUNTRY.Austria;

		if (country.equals("Azerbaijan"))
			c = COUNTRY.Azerbaijan;

		if (country.equals("Bahamas"))
			c = COUNTRY.Bahamas;

		if (country.equals("Bahrain"))
			c = COUNTRY.Bahrain;

		if (country.equals("Bangladesh"))
			c = COUNTRY.Bangladesh;

		if (country.equals("Barbados"))
			c = COUNTRY.Barbados;

		if (country.equals("Belarus"))
			c = COUNTRY.Belarus;

		if (country.equals("Belgium"))
			c = COUNTRY.Belgium;

		if (country.equals("Belize"))
			c = COUNTRY.Belize;

		if (country.equals("Benin"))
			c = COUNTRY.Benin;

		if (country.equals("Bermuda"))
			c = COUNTRY.Bermuda;

		if (country.equals("Bhutan"))
			c = COUNTRY.Bhutan;

		if (country.equals("Bolivia"))
			c = COUNTRY.Bolivia;

		if (country.equals("Bosnia-Herzegovina"))
			c = COUNTRY.Bosnia_Herzegovina;

		if (country.equals("Botswana"))
			c = COUNTRY.Botswana;

		if (country.equals("Brazil"))
			c = COUNTRY.Brazil;

		if (country.equals("Brunei"))
			c = COUNTRY.Brunei;

		if (country.equals("Bulgaria"))
			c = COUNTRY.Bulgaria;

		if (country.equals("Burkina Faso"))
			c = COUNTRY.Burkina_Faso;

		if (country.equals("Burundi"))
			c = COUNTRY.Burundi;

		if (country.equals("Cambodia"))
			c = COUNTRY.Cambodia;

		if (country.equals("Cameroon"))
			c = COUNTRY.Cameroon;

		if (country.equals("Canada"))
			c = COUNTRY.Canada;

		if (country.equals("Cape Verde"))
			c = COUNTRY.Cape_Verde;

		if (country.equals("Cayman Islands"))
			c = COUNTRY.Cayman_Islands;

		if (country.equals("Central Africa"))
			c = COUNTRY.Central_Africa;

		if (country.equals("Chad"))
			c = COUNTRY.Chad;

		if (country.equals("Chile"))
			c = COUNTRY.Chile;

		if (country.equals("China"))
			c = COUNTRY.China;

		if (country.equals("Colombia"))
			c = COUNTRY.Colombia;

		if (country.equals("Comoros"))
			c = COUNTRY.Comoros;

		if (country.equals("Congo"))
			c = COUNTRY.Congo;

		if (country.equals("Congo (Dem. Rep.)"))
			c = COUNTRY.Congo_Dem_Rep;

		if (country.equals("Cook Islands"))
			c = COUNTRY.Cook_Islands;

		if (country.equals("Costa Rica"))
			c = COUNTRY.Costa_Rica;

		if (country.equals("Croatia"))
			c = COUNTRY.Croatia;

		if (country.equals("Cuba"))
			c = COUNTRY.Cuba;

		if (country.equals("Cyprus"))
			c = COUNTRY.Cyprus;

		if (country.equals("Czech Republic"))
			c = COUNTRY.Czech_Republic;

		if (country.equals("Denmark"))
			c = COUNTRY.Denmark;

		if (country.equals("Djibouti"))
			c = COUNTRY.Djibouti;

		if (country.equals("Dominica"))
			c = COUNTRY.Dominica;

		if (country.equals("Dominican Republic"))
			c = COUNTRY.Dominican_Republic;

		if (country.equals("East Timor"))
			c = COUNTRY.East_Timor;

		if (country.equals("Ecuador"))
			c = COUNTRY.Ecuador;

		if (country.equals("Egypt"))
			c = COUNTRY.Egypt;

		if (country.equals("El Salvador"))
			c = COUNTRY.El_Salvador;

		if (country.equals("Equatorial Guinea"))
			c = COUNTRY.Equatorial_Guinea;

		if (country.equals("Eritrea"))
			c = COUNTRY.Eritrea;

		if (country.equals("Estonia"))
			c = COUNTRY.Estonia;

		if (country.equals("Ethiopia"))
			c = COUNTRY.Ethiopia;

		if (country.equals("Falkland Islands and dependencies"))
			c = COUNTRY.Falkland_Islands_and_dependencies;

		if (country.equals("Faroe Islands"))
			c = COUNTRY.Faroe_Islands;

		if (country.equals("Fiji"))
			c = COUNTRY.Fiji;

		if (country.equals("Finland"))
			c = COUNTRY.Finland;

		if (country.equals("France"))
			c = COUNTRY.France;

		if (country.equals("French Guiana"))
			c = COUNTRY.French_Guiana;

		if (country.equals("French Polynesia"))
			c = COUNTRY.French_Polynesia;

		if (country.equals("Gabon"))
			c = COUNTRY.Gabon;

		if (country.equals("Gambia"))
			c = COUNTRY.Gambia;

		if (country.equals("Georgia"))
			c = COUNTRY.Georgia;

		if (country.equals("Germany"))
			c = COUNTRY.Germany;

		if (country.equals("Ghana"))
			c = COUNTRY.Ghana;

		if (country.equals("Gibraltar"))
			c = COUNTRY.Gibraltar;

		if (country.equals("Greece"))
			c = COUNTRY.Greece;

		if (country.equals("Greenland"))
			c = COUNTRY.Greenland;

		if (country.equals("Grenada"))
			c = COUNTRY.Grenada;

		if (country.equals("Guadeloupe"))
			c = COUNTRY.French_Guadeloupe;

		if (country.equals("Guam"))
			c = COUNTRY.Guam;

		if (country.equals("Guatemala"))
			c = COUNTRY.Guatemala;

		if (country.equals("Guernsey and Alderney"))
			c = COUNTRY.Guernsey_and_Alderney;

		if (country.equals("Guinea"))
			c = COUNTRY.Guinea;

		if (country.equals("Guinea-Bissau"))
			c = COUNTRY.Guinea_Bissau;

		if (country.equals("Guyana"))
			c = COUNTRY.Guyana;

		if (country.equals("Haiti"))
			c = COUNTRY.Haiti;

		if (country.equals("Honduras"))
			c = COUNTRY.Honduras;

		if (country.equals("Hungary"))
			c = COUNTRY.Hungary;

		if (country.equals("Iceland"))
			c = COUNTRY.Iceland;

		if (country.equals("India"))
			c = COUNTRY.India;

		if (country.equals("Indonesia"))
			c = COUNTRY.Indonesia;

		if (country.equals("Iran"))
			c = COUNTRY.Iran;

		if (country.equals("Iraq"))
			c = COUNTRY.Iraq;

		if (country.equals("Ireland"))
			c = COUNTRY.Ireland;

		if (country.equals("Israel"))
			c = COUNTRY.Israel;

		if (country.equals("Italy"))
			c = COUNTRY.Italy;

		if (country.equals("Ivory Coast"))
			c = COUNTRY.Ivory_Coast;

		if (country.equals("Jamaica"))
			c = COUNTRY.Jamaica;

		if (country.equals("Japan"))
			c = COUNTRY.Japan;

		if (country.equals("Jersey"))
			c = COUNTRY.Jersey;

		if (country.equals("Jordan"))
			c = COUNTRY.Jordan;

		if (country.equals("Kazakhstan"))
			c = COUNTRY.Kazakhstan;

		if (country.equals("Kenya"))
			c = COUNTRY.Kenya;

		if (country.equals("Kiribati"))
			c = COUNTRY.Kiribati;

		if (country.equals("Korea of North"))
			c = COUNTRY.Korea_of_North;

		if (country.equals("Korea of South"))
			c = COUNTRY.Korea_of_South;

		if (country.equals("Kuwait"))
			c = COUNTRY.Kuwait;

		if (country.equals("Kyrgyzstan"))
			c = COUNTRY.Kyrgyzstan;

		if (country.equals("Laos"))
			c = COUNTRY.Laos;

		if (country.equals("Latvia"))
			c = COUNTRY.Latvia;

		if (country.equals("Lebanon"))
			c = COUNTRY.Lebanon;

		if (country.equals("Lesotho"))
			c = COUNTRY.Lesotho;

		if (country.equals("Liberia"))
			c = COUNTRY.Liberia;

		if (country.equals("Libya"))
			c = COUNTRY.Libya;

		if (country.equals("Liechtenstein"))
			c = COUNTRY.Liechtenstein;

		if (country.equals("Lithuania"))
			c = COUNTRY.Lithuania;

		if (country.equals("Luxembourg"))
			c = COUNTRY.Luxembourg;

		if (country.equals("Macedonia"))
			c = COUNTRY.Macedonia;

		if (country.equals("Madagascar"))
			c = COUNTRY.Madagascar;

		if (country.equals("Malawi"))
			c = COUNTRY.Malawi;

		if (country.equals("Malaysia"))
			c = COUNTRY.Malaysia;

		if (country.equals("Maldives"))
			c = COUNTRY.Maldives;

		if (country.equals("Mali"))
			c = COUNTRY.Mali;

		if (country.equals("Malta"))
			c = COUNTRY.Malta;

		if (country.equals("Man (Isle of)"))
			c = COUNTRY.Man_Isle_of;

		if (country.equals("Marshall Islands"))
			c = COUNTRY.Marshall_Islands;

		if (country.equals("Martinique"))
			c = COUNTRY.French_Martinique;

		if (country.equals("Mauritania"))
			c = COUNTRY.Mauritania;

		if (country.equals("Mauritius"))
			c = COUNTRY.Mauritius;

		if (country.equals("Mayotte"))
			c = COUNTRY.Mayotte;

		if (country.equals("Mexico"))
			c = COUNTRY.Mexico;

		if (country.equals("Micronesia"))
			c = COUNTRY.Micronesia;

		if (country.equals("Moldova"))
			c = COUNTRY.Moldova;

		if (country.equals("Monaco"))
			c = COUNTRY.Monaco;

		if (country.equals("Mongolia"))
			c = COUNTRY.Mongolia;

		if (country.equals("Montenegro"))
			c = COUNTRY.Montenegro;

		if (country.equals("Montserrat"))
			c = COUNTRY.Montserrat;

		if (country.equals("Morocco"))
			c = COUNTRY.Morocco;

		if (country.equals("Mozambique"))
			c = COUNTRY.Mozambique;

		if (country.equals("Myanmar"))
			c = COUNTRY.Myanmar;

		if (country.equals("Namibia"))
			c = COUNTRY.Namibia;

		if (country.equals("Nauru"))
			c = COUNTRY.Nauru;

		if (country.equals("Nepal"))
			c = COUNTRY.Nepal;

		if (country.equals("Netherlands"))
			c = COUNTRY.Netherlands;

		if (country.equals("Netherlands Antilles"))
			c = COUNTRY.Netherlands_Antilles;

		if (country.equals("New Caledonia"))
			c = COUNTRY.New_Caledonia;

		if (country.equals("New Zealand"))
			c = COUNTRY.New_Zealand;

		if (country.equals("Nicaragua"))
			c = COUNTRY.Nicaragua;

		if (country.equals("Niger"))
			c = COUNTRY.Niger;

		if (country.equals("Nigeria"))
			c = COUNTRY.Nigeria;

		if (country.equals("Niue"))
			c = COUNTRY.Niue;

		if (country.equals("Norfolk Island"))
			c = COUNTRY.Norfolk_Island;

		if (country.equals("Northern Mariana Islands"))
			c = COUNTRY.Northern_Mariana_Islands;

		if (country.equals("Norway"))
			c = COUNTRY.Norway;

		if (country.equals("Oman"))
			c = COUNTRY.Oman;

		if (country.equals("Pakistan"))
			c = COUNTRY.Pakistan;

		if (country.equals("Palau"))
			c = COUNTRY.Palau;

		if (country.equals("Palestine"))
			c = COUNTRY.Palestine;

		if (country.equals("Panama"))
			c = COUNTRY.Panama;

		if (country.equals("Papua New Guinea"))
			c = COUNTRY.Papua_New_Guinea;

		if (country.equals("Paraguay"))
			c = COUNTRY.Paraguay;

		if (country.equals("Peru"))
			c = COUNTRY.Peru;

		if (country.equals("Philippines"))
			c = COUNTRY.Philippines;

		if (country.equals("Poland"))
			c = COUNTRY.Poland;

		if (country.equals("Portugal"))
			c = COUNTRY.Portugal;

		if (country.equals("Puerto Rico"))
			c = COUNTRY.Puerto_Rico;

		if (country.equals("Qatar"))
			c = COUNTRY.Qatar;

		if (country.equals("R\u00e9union"))
			c = COUNTRY.Reunion;

		if (country.equals("Romania"))
			c = COUNTRY.Romania;

		if (country.equals("Russia"))
			c = COUNTRY.Russia;

		if (country.equals("Rwanda"))
			c = COUNTRY.Rwanda;

		if (country.equals("Sahara"))
			c = COUNTRY.Sahara;

		if (country.equals("Saint Helena"))
			c = COUNTRY.Saint_Helena;

		if (country.equals("Saint Kitts and Nevis"))
			c = COUNTRY.Saint_Kitts_and_Nevis;

		if (country.equals("Saint Lucia"))
			c = COUNTRY.Saint_Lucia;

		if (country.equals("Saint Pierre & Miquelon"))
			c = COUNTRY.Saint_Pierre_and_Miquelon;

		if (country.equals("Saint Vincent and the Grenadines"))
			c = COUNTRY.Saint_Vincent_and_the_Grenadines;

		if (country.equals("Samoa"))
			c = COUNTRY.Samoa;

		if (country.equals("San Marino"))
			c = COUNTRY.San_Marino;

		if (country.equals("S\u00e3o Tom\u00e9 and Pr\u00edncipe"))
			c = COUNTRY.Sao_Tome_and_Principe;

		if (country.equals("Saudi Arabia"))
			c = COUNTRY.Saudi_Arabia;

		if (country.equals("Senegal"))
			c = COUNTRY.Senegal;

		if (country.equals("Serbia"))
			c = COUNTRY.Serbia;

		if (country.equals("Seychelles"))
			c = COUNTRY.Seychelles;

		if (country.equals("Sierra Leone"))
			c = COUNTRY.Sierra_Leone;

		if (country.equals("Singapore"))
			c = COUNTRY.Singapore;

		if (country.equals("Slovakia"))
			c = COUNTRY.Slovakia;

		if (country.equals("Slovenia"))
			c = COUNTRY.Slovenia;

		if (country.equals("Solomon Islands"))
			c = COUNTRY.Solomon_Islands;

		if (country.equals("Somalia"))
			c = COUNTRY.Somalia;

		if (country.equals("South Africa Republic"))
			c = COUNTRY.South_Africa_Republic;

		if (country.equals("Spain") || country.equals("Espa\u00f1a"))
			c = COUNTRY.Spain;

		if (country.equals("Sri Lanka"))
			c = COUNTRY.Sri_Lanka;

		if (country.equals("Sudan"))
			c = COUNTRY.Sudan;

		if (country.equals("Sudan of South"))
			c = COUNTRY.SudanOfSouth;

		if (country.equals("Suriname"))
			c = COUNTRY.Suriname;

		if (country.equals("Svalbard and Jan Mayen"))
			c = COUNTRY.Svalbard_and_Jan_Mayen;

		if (country.equals("Swaziland"))
			c = COUNTRY.Swaziland;

		if (country.equals("Sweden"))
			c = COUNTRY.Sweden;

		if (country.equals("Switzerland"))
			c = COUNTRY.Switzerland;

		if (country.equals("Syria"))
			c = COUNTRY.Syria;

		if (country.equals("Taiwan"))
			c = COUNTRY.Taiwan;

		if (country.equals("Tajikistan"))
			c = COUNTRY.Tajikistan;

		if (country.equals("Tanzania"))
			c = COUNTRY.Tanzania;

		if (country.equals("Terres Australes"))
			c = COUNTRY.Terres_Australes;

		if (country.equals("Thailand"))
			c = COUNTRY.Thailand;

		if (country.equals("Togo"))
			c = COUNTRY.Togo;

		if (country.equals("Tokelau"))
			c = COUNTRY.Tokelau;

		if (country.equals("Tonga"))
			c = COUNTRY.Tonga;

		if (country.equals("Trinidad and Tobago"))
			c = COUNTRY.Trinidad_and_Tobago;

		if (country.equals("Tunisia"))
			c = COUNTRY.Tunisia;

		if (country.equals("Turkey"))
			c = COUNTRY.Turkey;

		if (country.equals("Turkmenistan"))
			c = COUNTRY.Turkmenistan;

		if (country.equals("Turks and Caicos Islands"))
			c = COUNTRY.Turks_and_Caicos_Islands;

		if (country.equals("Tuvalu"))
			c = COUNTRY.Tuvalu;

		if (country.equals("Uganda"))
			c = COUNTRY.Uganda;

		if (country.equals("Ukraine"))
			c = COUNTRY.Ukraine;

		if (country.equals("United Arab Emirates"))
			c = COUNTRY.United_Arab_Emirates;

		if (country.equals("United Kingdom"))
			c = COUNTRY.United_Kingdom;

		if (country.equals("United States of America"))
			c = COUNTRY.United_States_of_America;

		if (country.equals("Uruguay"))
			c = COUNTRY.Uruguay;

		if (country.equals("Uzbekistan"))
			c = COUNTRY.Uzbekistan;

		if (country.equals("Vanuatu"))
			c = COUNTRY.Vanuatu;

		if (country.equals("Vatican"))
			c = COUNTRY.Vatican;

		if (country.equals("Venezuela"))
			c = COUNTRY.Venezuela;

		if (country.equals("Vietnam"))
			c = COUNTRY.Vietnam;

		if (country.equals("Virgin Islands of the UK"))
			c = COUNTRY.Virgin_Islands_of_the_UK;

		if (country.equals("Virgin Islands of the United States"))
			c = COUNTRY.Virgin_Islands_of_the_United_States;

		if (country.equals("Wallis & Futuna"))
			c = COUNTRY.Wallis_and_Futuna;

		if (country.equals("Yemen"))
			c = COUNTRY.Yemen;

		if (country.equals("Zambia"))
			c = COUNTRY.Zambia;

		if (country.equals("Zimbabwe"))
			c = COUNTRY.Zimbabwe;

		if (c == null)
			throw new JPARSECException("country " + country + " not found.");

		return c;
	}
}
