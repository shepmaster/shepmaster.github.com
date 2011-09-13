#include <stdio.h>
#include <stdlib.h>

void
foo(int do_it) {
  int value;

  if (do_it) {
    value = 7;
  }

  if (7 == value) {
    printf("It is 7\n");
  }
}

int
main(int argc, char *argv[])
{
  foo(argc < 2);

  return 0;
}
