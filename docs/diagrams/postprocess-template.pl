#!/usr/bin/env perl

use strict;
use warnings;

my ($path) = @ARGV;
die "Usage: $0 <svg>\n" unless defined $path;

open my $input, '<', $path or die "Cannot read $path: $!\n";
local $/;
my $svg = <$input>;
close $input;

my ($marker_id) = $svg =~ /<marker id="([^"]+)"/;
die "Cannot find the D2 arrow marker in $path\n" unless defined $marker_id;
my ($mask_id) = $svg =~ /<mask id="([^"]+)"/;
die "Cannot find the D2 connection mask in $path\n" unless defined $mask_id;

sub replace_once {
  my ($pattern, $replacement, $description) = @_;
  my $count = ($svg =~ s/$pattern/$replacement/);
  die "Cannot update $description in $path\n" unless $count == 1;
}

replace_once(qr/viewBox="0 0 894 482"/, 'viewBox="0 0 894 700"', 'outer view box');
replace_once(
  qr/width="894" height="482" viewBox="-17 -17 894 482"/,
  'width="894" height="700" viewBox="-17 -100 894 700"',
  'inner view box',
);
replace_once(
  qr/<rect x="-17\.000000" y="-17\.000000" width="894\.000000" height="482\.000000"/,
  '<rect x="-17.000000" y="-100.000000" width="894.000000" height="700.000000"',
  'background',
);
replace_once(
  qr/<mask id="([^"]+)" maskUnits="userSpaceOnUse" x="-17" y="-17" width="894" height="482">/,
  qq{<mask id="$mask_id" maskUnits="userSpaceOnUse" x="-17" y="-100" width="894" height="700">},
  'mask bounds',
);
replace_once(
  qr/<rect x="-17" y="-17" width="894" height="482" fill="white"><\/rect>/,
  '<rect x="-17" y="-100" width="894" height="700" fill="white"></rect>',
  'mask background',
);

my $loops = <<"SVG";
<g class="template-feedback-loops">
  <path d="M 225 0 L 225 -35 L 75 -35 L 75 -4" stroke="#222222" fill="none" class="connection" style="stroke-width:2;" marker-end="url(#$marker_id)" />
  <text x="150" y="-78" fill="#222222" class="text-mono-italic" style="text-anchor:middle;font-size:18px"><tspan x="150" dy="0">Compute</tspan><tspan x="150" dy="20.5">new model</tspan></text>
  <path d="M 785 0 L 785 -35 L 635 -35 L 635 -4" stroke="#222222" fill="none" class="connection" style="stroke-width:2;" marker-end="url(#$marker_id)" />
  <text x="710" y="-78" fill="#222222" class="text-mono-italic" style="text-anchor:middle;font-size:18px"><tspan x="710" dy="0">Render model</tspan><tspan x="710" dy="20.5">on screen</tspan></text>
  <path d="M 75 452 L 75 570 L 225 570 L 225 452" stroke="#222222" fill="none" class="connection" style="stroke-width:2;" marker-end="url(#$marker_id)" />
  <text x="150" y="490" fill="#222222" class="text-mono-italic" style="text-anchor:middle;font-size:18px"><tspan x="150" dy="0">Combine</tspan><tspan x="150" dy="20.5">models to</tspan><tspan x="150" dy="20.5">template</tspan></text>
</g>
SVG

replace_once(qr/<mask id=/, "$loops<mask id=", 'feedback loops');

open my $output, '>', $path or die "Cannot write $path: $!\n";
print {$output} $svg;
close $output;
