#!/usr/bin/env perl

use strict;
use warnings;

my ($path) = @ARGV;
die "Usage: $0 <svg>\n" unless defined $path;

open my $input, '<', $path or die "Cannot read $path: $!\n";
local $/;
my $svg = <$input>;
close $input;

sub replace_once {
  my ($pattern, $replacement, $description) = @_;
  my $count = ($svg =~ s{$pattern}{
    my $capture = defined $1 ? $1 : '';
    my $expanded = $replacement;
    $expanded =~ s{\$1}{$capture}g;
    $expanded;
  }ex);
  die "Cannot update $description in $path\n" unless $count == 1;
}

replace_once(
  qr/<rect x="0\.000000" y="0\.000000" width="640\.000000" height="140\.000000"([^>]*)\/>/,
  '<polygon points="320,0 400,140 240,140"$1/>',
  'E2E section',
);
replace_once(
  qr/<rect x="0\.000000" y="140\.000000" width="640\.000000" height="140\.000000"([^>]*)\/>/,
  '<polygon points="240,140 400,140 480,280 160,280"$1/>',
  'integration section',
);
replace_once(
  qr/<rect x="0\.000000" y="280\.000000" width="640\.000000" height="140\.000000"([^>]*)\/>/,
  '<polygon points="160,280 480,280 560,420 80,420"$1/>',
  'instrumented section',
);
replace_once(
  qr/<rect x="0\.000000" y="420\.000000" width="640\.000000" height="140\.000000"([^>]*)\/>/,
  '<polygon points="80,420 560,420 640,560 0,560"$1/>',
  'unit section',
);

open my $output, '>', $path or die "Cannot write $path: $!\n";
print {$output} $svg;
close $output;
