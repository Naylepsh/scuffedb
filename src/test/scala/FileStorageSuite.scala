class FileStorageSuite extends munit.FunSuite:
  test("merge"):
    val files: List[List[Line]] = List(
      List(
        Line.make("handbag", "8786"),
        Line.make("handful", "40308"),
        Line.make("handicap", "65955"),
        Line.make("handkerchief", "16324"),
        Line.make("handlebars", "3869"),
        Line.make("handprinted", "11150")
      ),
      List(
        Line.make("handcuffs", "2729"),
        Line.make("handful", "42307"),
        Line.make("handicap", "67884"),
        Line.make("handiwork", "16912"),
        Line.make("handkerchief", "20952"),
        Line.make("handprinted", "15725")
      ),
      List(
        Line.make("handful", "44662"),
        Line.make("handicap", "70836"),
        Line.make("handiwork", "45521"),
        Line.make("handlebars", "3869"),
        Line.make("handoff", "5754"),
        Line.make("handprinted", "33632")
      )
    ).reverse
    val expected = List(
      Line.make("handbag", "8786"),
      Line.make("handcuffs", "2729"),
      Line.make("handful", "44662"),
      Line.make("handicap", "70836"),
      Line.make("handiwork", "45521"),
      Line.make("handkerchief", "20952"),
      Line.make("handlebars", "3869"),
      Line.make("handoff", "5754"),
      Line.make("handprinted", "33632")
    )

    val actual = FileStorage.merge(files)
    println(actual)
    assertEquals(actual, expected)
