# Provider mapping semantics — October 8, 2026

This is the field disposition for the shared resource artifact. The mapping resources remain the
single executable definitions. FEI admission, simple workers and correlated workers use the same
supplemental transform registry. Source schemas, output schemas and index mappings are distinct.

## WorldCheck

Tab-separated, header present, ISO-8859-1 (supplier confirmation of Latin-1 versus CP1252 is still
outstanding). All 35 columns are strings. UID and CATEGORY are required. Names must contain at least
one usable value after mapping. Unknown extra columns and every original value, including blanks,
remain in `source_data`. Its children are stored-only unless explicitly indexed.

| Source column | Canonical targets |
|---|---|
| `UID` | `uid` |
| `LAST NAME` | `names` |
| `FIRST NAME` | `names` |
| `ALIASES` | `names` |
| `LOW QUALITY ALIASES` | `names` |
| `ALTERNATIVE SPELLING` | `names` |
| `CATEGORY` | `entity_kind`, `source_categories` |
| `TITLE` | `title` |
| `SUB-CATEGORY` | `source_subcategories` |
| `POSITION` | `position` |
| `AGE` | `age` |
| `DOB` | `dates`, `date_details` |
| `DOBS` | `dates`, `date_details` |
| `PLACE OF BIRTH` | `place_of_birth` |
| `DECEASED` | `deceased` |
| `PASSPORTS` | `identifiers` |
| `SSN` | `identifiers` |
| `IDENTIFICATION NUMBERS` | `identifiers` |
| `LOCATIONS` | `locations`, `location_country_codes` |
| `COUNTRIES` | `countries` |
| `CITIZENSHIP` | `nationalities` |
| `COMPANIES` | `company_ids` |
| `E/I` | `entity_kind` |
| `LINKED TO` | `linked_record_ids` |
| `FURTHER INFORMATION` | `further_information` |
| `KEYWORDS` | `keywords` |
| `EXTERNAL SOURCES` | `external_sources`, `external_source_domains` |
| `UPDATE CATEGORY` | `update_category` |
| `ENTERED` | `entered_date` |
| `UPDATED` | `updated_date` |
| `EDITOR` | `editor` |
| `AGE DATE (AS OF DATE)` | `age_as_of_date` |
| `PEP ROLES` | `pep_roles` |
| `PEP STATUS` | `pep_status` |
| `SPECIAL INTEREST CATEGORIES` | `special_interest_categories`, `categories` |

The synthetic `worldcheck-all-fields-synthetic.json` fixture provides an independently specified full
document, not merely a header-width assertion. Real OpenSearch tests compare the complete persisted
document and query partial dates, city and unknown classification. The external ten-row sample is
an opt-in bounded test input, never copied into Git; it checks validation, raw equality and retention
of bare passports without logging records.

Semantics:

- One primary name combines surname and first name; surname is a fallback only when first name is
  absent. Aliases split on semicolon, trim and deduplicate case-insensitively, with primary/high-quality
  names taking precedence. Alternative spelling is an alias. Low-quality aliases remain stored and
  recallable but the matching decoder excludes them from strong name evidence.
- `dates` contains only valid complete calendar dates. DOB and all semicolon-separated DOBS contribute
  unique values. `date_details` retains YEAR/MONTH/DAY precision. Zero month/day is unknown, never
  January/first-of-month. Invalid dates and contradictory zero-month/nonzero-day are raw-only.
  Partial dates are explicitly indexed for queries but do not enter the current exact-date scorer.
- E/I remains authoritative for person versus non-person; CATEGORY refines entity kind. Category,
  subcategory, PEP status/roles and special-interest labels remain separately available. Known risk
  categories normalize to `categories`; unknown labels remain searchable in `special_interest_categories`.
  Classification is provider evidence, not an assertion that a person committed an offense.
- Locations retain city, region, original country name and resolvable ISO2 country; address purpose is
  not fabricated. Unparseable locations and unknown country labels remain in raw source data.
- Brace-qualified identifiers retain their schemes. Bare passports receive PASSPORT, bare untyped
  identification numbers OTHER, SSNs US_SSN. Lists split on semicolon. Scheme-specific normalization
  applies where known; punctuation/issuer annotations in unrecognized and passport formats are
  preserved rather than guessed away. Malformed braces remain raw-only. Masked or incomplete US
  SSN/EIN/ITIN values must not become exact identifiers from a suffix.
- Countries/nationalities resolve known names to ISO2; unknown values remain raw. Companies and links
  are provider record-ID lists, not name strings or automatically materialized entity relationships.
- Entered/updated/age-as-of dates require complete valid Y/M/D. Age is a nonnegative integer. Deceased
  recognizes Y/N, YES/NO, TRUE/FALSE, 1/0 and DECEASED; unknown spellings are raw-only, not false.
- Keywords and PEP roles split on tilde; external URLs and domains use the shared URL parser. Narrative,
  title, position, birthplace, editor and update category retain provider text. Update category is
  descriptive; it does not implement deletion/tombstones.
- All canonical fields have explicit strict index mappings. Keyword extras use ignore_above=2048;
  longer originals still survive in `_source`. Source-data indexing remains opt-in. These fields are
  available as evidence/search data; this change does not add dedicated Studio widgets or risk filters.

## State Street customer master

Pipe-separated UTF-8, 42 distinct named columns. The existing reader's duplicate trailing headers
are last-column-wins, including blank values; the original file remains the physical-column audit
source. `source_data` is the logical named record, not a preservation format for duplicate positions.
No second conflicting customer source schema is introduced. The destination here is FEI resolution
assertions, not the screening index. A future screening destination must reuse this source definition.

| Source column | Resolution disposition |
|---|---|
| `ENTITYS_UNIQUE_ID` | `uid`; raw `source_data` |
| `BUSINESS_ENTITY_RECORD_ID` | `business_entity_record_id@STATE_STREET_CUSTOMER_MASTER`; raw `source_data` |
| `GEMS_ID` | `kyc_id@STATE_STREET_CUSTOMER_MASTER`; raw `source_data` |
| `ENTITYS_FULL_NAME` | `display_name`, `name`; raw `source_data` |
| `ENTITY_PREFIX` | `display_name`, `name_prefix`; raw `source_data` |
| `ENTITY_FIRST_NAME` | `display_name`, `given_name`; raw `source_data` |
| `ENTITY_MIDDLE_NAME` | `display_name`, `middle_name`; raw `source_data` |
| `ENTITY_LAST_NAME` | `display_name`, `family_name`; raw `source_data` |
| `ENTITY_SUFFIX` | `display_name`, `name_suffix`; raw `source_data` |
| `TRADE_AS_NAME` | `trading_name`; raw `source_data` |
| `FORMER_NAME` | `former_name`; raw `source_data` |
| `ENTITY_LOCATION_1_ADDRESS_LINE_1` | `registered_address`; raw `source_data` |
| `ENTITY_LOCATION_1_ADDRESS_LINE_2` | `registered_address`; raw `source_data` |
| `ENTITY_LOCATION_1_CITY` | `registered_address`; raw `source_data` |
| `ENTITY_LOCATION_1_STATE_PROVINCE` | `registered_address`; raw `source_data` |
| `ENTITY_LOCATION_1_COUNTRY` | `primary_jurisdiction`, `registered_address`; raw `source_data` |
| `ENTITY_LOCATION_2_ADDRESS_LINE_1` | `mailing_address`; raw `source_data` |
| `ENTITY_LOCATION_2_ADDRESS_LINE_2` | `mailing_address`; raw `source_data` |
| `ENTITY_LOCATION_2_CITY` | `mailing_address`; raw `source_data` |
| `ENTITY_LOCATION_2_STATE_PROVINCE` | `mailing_address`; raw `source_data` |
| `ENTITY_LOCATION_2_COUNTRY` | `mailing_address`; raw `source_data` |
| `ENTITY_LOCATION_3_ADDRESS_LINE_1` | `other_address`; raw `source_data` |
| `ENTITY_LOCATION_3_ADDRESS_LINE_2` | `other_address`; raw `source_data` |
| `ENTITY_LOCATION_3_CITY` | `other_address`; raw `source_data` |
| `ENTITY_LOCATION_3_STATE_PROVINCE` | `other_address`; raw `source_data` |
| `ENTITY_LOCATION_3_COUNTRY` | `other_address`; raw `source_data` |
| `COUNTRY_OF_CITIZENSHIP_INCORPORATION` | `citizenship_or_incorporation_country`, `identity_assertions`; raw `source_data` |
| `COUNTRY_OF_NATIONALITY` | `nationality`; raw `source_data` |
| `DATE_OF_BIRTH_DATE_OF_INCORPORATION` | `date_of_birth_or_incorporation`, `identity_assertions`; raw `source_data` |
| `OCCUPATION` | `further_information`, `occupation`; raw `source_data` |
| `JOB_TITLE` | `position`; raw `source_data` |
| `LAST_UPDATED_DATE` | `source_updated_date`; raw `source_data` |
| `ENTITY_TYPE_CODE` | `source_entity_type_code`; raw `source_data` |
| `ENTITYS_TYPE` | `entity_kind`, `source_entity_type`, `identity_assertions`; raw `source_data` |
| `ENTITY_PUBLIC_IDENTIFIER` | `public_identifier_assertions`; raw `source_data` |
| `PRIMARY_BUSINESS_UNIT` | `further_information`, `primary_business_unit`; raw `source_data` |
| `CONTRACTING_ENTITIES_AND_LOCATIONS` | `further_information`, `contracting_entities_and_locations`; raw `source_data` |
| `SERVICING_ENTITIES_AND_LOCATIONS` | `further_information`, `servicing_entities_and_locations`; raw `source_data` |
| `BOOKING_ENTITIES_AND_LOCATIONS` | `further_information`, `booking_entities_and_locations`; raw `source_data` |
| `ADDITIONAL_BUSINESS_UNITS_AND_LOCATIONS` | `further_information`, `additional_business_units_and_locations`; raw `source_data` |
| `ENTITY_CLASSIFICATION` | `further_information`, `entity_classification`; raw `source_data` |
| `PLACE_OF_BIRTH` | `place_of_birth`; raw `source_data` |

- Full display name wins; when blank, prefix/given/middle/family/suffix form the fallback. The original
  components remain assertions. Trade/former names become separate `trading_name`/`former_name`
  assertions for each semicolon-delimited name, using the actual resolution evidence-extractor keys.
- Individual maps to person. Known organization labels (including the established Trust (Corp & Ind)
  treatment) map to organization. Unrecognized types fail the source-of-truth output contract rather
  than silently becoming organizations. Raw type/code/classification remain available for accepted rows.
- Strict yyyyMMdd birth/incorporation dates retain the legacy combined assertion and also produce
  `date_of_birth` for people / `date_of_incorporation` for organizations. Matching consumes both date
  keys. Invalid optional dates remain only in the raw assertion. LAST_UPDATED_DATE is an ISO date.
- Citizenship/incorporation country retains its original combined assertion and produces canonical
  citizenship/incorporation-jurisdiction plus country evidence. Three addresses retain their components
  and established registered/mailing/other position semantics. Unknown country names survive raw.
- Public identifiers carry a real IdentifierScheme when recognized; otherwise domain
  STATE_STREET_CUSTOMER_MASTER plus identifier_type. Business-record and KYC IDs are explicitly
  domain-scoped too. The persisted keys are business_entity_record_id@STATE_STREET_CUSTOMER_MASTER,
  kyc_id@STATE_STREET_CUSTOMER_MASTER and, for example, tin@STATE_STREET_CUSTOMER_MASTER. LEI stays lei.
  Do not retain the old test-prefixed keys as proof that identifiers are usable by deterministic matching.
- Position, occupation, classification, business units, servicing/booking/contracting and birthplace
  are separate assertions; the legacy combined narrative also remains. No company/location relationship
  graph is inferred from descriptive business strings.
- An opt-in source-contract setting `preserve_source_assertion: true` creates a JSON `source_data`
  assertion using the existing typed-assertion wire contract. All original named fields, blanks and
  unknown extra columns survive resolution persistence. Dates are plain ISO strings at the mapping
  boundary, avoiding framework-specific Jackson timestamp arrays.

## Compatibility and rollout

Shared resource URIs are stable; admitted snapshots contain their own bytes/digest. New rich location
and identifier transforms have new names, retaining old implementations for replay. Decode no longer
compares an admitted index against today's expanded standard; admission still enforces the standard.
No generated Java, deployed migration or existing index is edited. Use a fresh full WorldCheck
revision for this schema change; a delta against the old shape is intentionally incompatible.
For customer dev validation use a new source/pipeline key: existing resolved source-of-truth candidates
are intentionally skipped by replay, so replay does not backfill their new assertions. Migrating existing
customer populations requires a separate update/backfill operation, not a hidden mutation here.

These semantic fixes do not remove the full-export streaming and 1 GiB acquisition-limit blockers.
Dev tests should begin with the bounded external sample and representative customer rows. Matrix,
large-file, failure/recovery and shared API component work remain separately tracked.

## Full-export parsing evidence — October 8, 2026

The real full-export API run exposed literal quote characters at row 333 that generic CSV
encapsulation rejected. A streaming physical-line scan verified all 5,818,856 data lines have
35 tab-separated fields. The WorldCheck source contract now declares `source.quote: null` so
quotes remain source data, including leading/unmatched alias quotes. FDS and FEI/Spark propagate
this explicit dialect; absence of the property retains standard CSV quoting in older admitted
snapshots and other providers. Full-file execution evidence is recorded in FEI's
`docs/full-file-ingestion-repair-2026-10-08.md`; small-fixture passes do not close that acceptance.
