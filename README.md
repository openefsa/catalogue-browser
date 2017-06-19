# CatalogueBrowser
Source code of the catalogue browser java application.
Do NOT change the business_rules package name, since the Batch checking
tools use it to make the checks.

General rules for the Catalogue Browser:

1) Special attributes

Attributes with special names are treated separately.

 - "termType": identifies the type of term of terms. If set, its value is visualized in the
 main UI in the left combo box in the term tab (right side of main panel)
 
 - "detailLevel": same as termType, but it identifies the level of detail of terms. It is visualized
 in the main UI in the right combo box in the term tab
 
 - "implicitFacets": a repeatable attribute which is used to define the implicit facets of terms
 (not inherited, only the ones which belong to the term), as A070A#A080B$A0H7H$...
 
 - "allFacets": a NON repeatable attribute which is used to define all the implicit facets of terms
 (also inherited ones!). The format is the same as the implicit facets.


2) Defining termType values and labels

If a catalogue hosts the "termType" attribute, it is necessary to specify the
type of term values and labels in the scope notes of the attribute which identifies
the type of term attribute ( which should have as name "termType" ). The syntax is the following:

scopenotes$code=label$code=label$....

Example:
Here is the attribute scope notes$f=facet$g=generic term$r=raw primary commodity

The tool will parse the scope note and will extract the codes and labels of each term type.
Note that the order of insertion in the scope note is the same order which is displayed in the UI.


3) Default displayed hierarchy

The master hierarchy of a catalogue is the one which is displayed when we open a catalogue
for the first time.
Since the master hierarchy usually (but not always) should not be shown to the generic user (read-only),
it is possible to define a default hierarchy which is displayed as default one instead of the master hierarchy.
In order to do so, the catalogue scope note should be modified by introducing this information.

Syntax:
scopenotes$[hideMasterWith=HierarchyCode]

Example for the MTX catalogue:
This is the scope note of the catalogue$[hideMasterWith=report]

In this way we are hiding the master hierarchy of the MTX catalogue with the Reporting Hierarchy!


4) Term short name not present

If the term short name is not present, then the extended name is shown instead.
