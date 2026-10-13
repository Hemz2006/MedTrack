package com.medtrack.lookup

object Fixtures {
    const val SUGGEST_PANTAPRAZOLE = """{"approximateGroup":{"inputTerm":null,"candidate":[
        {"rxcui":"40790","rxaui":"10325229","score":"9.28","rank":"1","source":"GS"},
        {"rxcui":"40790","rxaui":"2072737","score":"9.28","rank":"1","name":"PANTOPRAZOLE","source":"VANDF"},
        {"rxcui":"40790","rxaui":"485118","score":"9.28","rank":"1","name":"pantoprazole","source":"RXNORM"},
        {"rxcui":"2100","rxaui":"1","score":"5.0","rank":"2","source":"MMSL"}]}}"""

    const val RXCUI_PANTOPRAZOLE = """{"idGroup":{"rxnormId":["40790"]}}"""
    const val RXCUI_NONE = """{"idGroup":{}}"""

    const val RELATED_PANTOPRAZOLE = """{"relatedGroup":{"rxcui":null,"conceptGroup":[
        {"tty":"IN","conceptProperties":[{"rxcui":"40790","name":"pantoprazole","tty":"IN"}]},
        {"tty":"SCD","conceptProperties":[
          {"rxcui":"251872","name":"pantoprazole 20 MG Delayed Release Oral Tablet","tty":"SCD"},
          {"rxcui":"2675781","name":"100 ML pantoprazole 0.4 MG/ML Injection","tty":"SCD"},
          {"rxcui":"283669","name":"pantoprazole 40 MG Injection","tty":"SCD"},
          {"rxcui":"314200","name":"pantoprazole 40 MG Delayed Release Oral Tablet","tty":"SCD"}]}]}}"""

    const val RELATED_APAP_CAFFEINE = """{"relatedGroup":{"rxcui":null,"conceptGroup":[
        {"tty":"IN","conceptProperties":[
          {"rxcui":"161","name":"acetaminophen","tty":"IN"},
          {"rxcui":"1886","name":"caffeine","tty":"IN"}]},
        {"tty":"SCD","conceptProperties":[
          {"rxcui":"106814","name":"acetaminophen 450 MG / caffeine 30 MG Oral Tablet","tty":"SCD"},
          {"rxcui":"106815","name":"acetaminophen 450 MG / caffeine 25 MG Oral Tablet","tty":"SCD"}]}]}}"""

    const val PUBCHEM_PANTOPRAZOLE = """{"PropertyTable":{"Properties":[{"CID":4679,"MolecularFormula":"C16H15F2N3O4S"}]}}"""
    const val PUBCHEM_ACETAMINOPHEN = """{"PropertyTable":{"Properties":[{"CID":1983,"MolecularFormula":"C8H9NO2"}]}}"""
    const val PUBCHEM_NOT_FOUND = """{"Fault":{"Code":"PUGREST.NotFound","Message":"No CID found"}}"""

    const val MEDLINE_PANTOPRAZOLE = """{"feed":{"title":{"_value":"MedlinePlus Connect"},"entry":[{"title":{"_value":"Pantoprazole"},
        "summary":{"type":"html","_value":"Pantoprazole is used to treat    gastroesophageal reflux disease (GERD; a condition where stomach acid flows back into the esophagus (tube between the stomach and throat)).  conditions where the stomach produces too much acid, such as Zollinger-Ellison syndrome Pantoprazole is in a class of medications called proton-pump inhibitors.It works by decreasing the amount of acid made in the stomach."}}]}}"""
    const val MEDLINE_ACETAMINOPHEN = """{"feed":{"entry":[{"title":{"_value":"Acetaminophen"},
        "summary":{"type":"html","_value":"<p>Acetaminophen is used to relieve mild to moderate pain.</p> <p>It is also used to reduce fever.</p> <p>Third sentence.</p>"}}]}}"""
    const val MEDLINE_EMPTY = """{"feed":{"entry":[]}}"""
}
