package scuffedb

import domain.*

class FileStorageSuite extends munit.FunSuite:
  test("merge all active"):
    val files: List[List[Entry]] = List(
      List(
        Entry.makeActive("handbag", "8786"),
        Entry.makeActive("handful", "40308"),
        Entry.makeActive("handicap", "65955"),
        Entry.makeActive("handkerchief", "16324"),
        Entry.makeActive("handlebars", "3869"),
        Entry.makeActive("handprinted", "11150")
      ),
      List(
        Entry.makeActive("handcuffs", "2729"),
        Entry.makeActive("handful", "42307"),
        Entry.makeActive("handicap", "67884"),
        Entry.makeActive("handiwork", "16912"),
        Entry.makeActive("handkerchief", "20952"),
        Entry.makeActive("handprinted", "15725")
      ),
      List(
        Entry.makeActive("handful", "44662"),
        Entry.makeActive("handicap", "70836"),
        Entry.makeActive("handiwork", "45521"),
        Entry.makeActive("handlebars", "3869"),
        Entry.makeActive("handoff", "5754"),
        Entry.makeActive("handprinted", "33632")
      )
    ).reverse
    val expected = List(
      Entry.makeActive("handbag", "8786"),
      Entry.makeActive("handcuffs", "2729"),
      Entry.makeActive("handful", "44662"),
      Entry.makeActive("handicap", "70836"),
      Entry.makeActive("handiwork", "45521"),
      Entry.makeActive("handkerchief", "20952"),
      Entry.makeActive("handlebars", "3869"),
      Entry.makeActive("handoff", "5754"),
      Entry.makeActive("handprinted", "33632")
    )

    val actual = FileStorage.merge(files)
    assertEquals(actual, expected)

  test("merge some tombstones"):
    val files: List[List[Entry]] = List(
      List(
        Entry.makeActive("handbag", "8784"),
        Entry.makeActive("handful", "40308"),
        Entry.makeTombstone("handicap"),
        Entry.makeActive("handprinted", "11150")
      ),
      List(
        Entry.makeTombstone("handbag"),
        Entry.makeActive("handful", "42307"),
        Entry.makeActive("handicap", "67884"),
        Entry.makeTombstone("handprinted")
      ),
      List(
        Entry.makeActive("handful", "44662"),
        Entry.makeActive("handicap", "70836"),
        Entry.makeActive("handprinted", "33632")
      )
    ).reverse
    val expected = List(
      Entry.makeActive("handful", "44662"),
      Entry.makeActive("handicap", "70836"),
      Entry.makeActive("handprinted", "33632")
    )

    val actual = FileStorage.merge(files)
    assertEquals(actual, expected)
