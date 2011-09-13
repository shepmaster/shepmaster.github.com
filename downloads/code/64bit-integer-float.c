#include <stdio.h>
#include <stdlib.h>

int
main(int argc, char *argv[])
{
  unsigned long long int i = -1;
  double d = i;
  unsigned long long int i2 = d;

  printf("%llu\n%.20g\n%llu\n", i, d, i2);
  return 0;
}
