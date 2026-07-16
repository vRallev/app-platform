use strict;
use warnings;

use MIME::Base64 qw(decode_base64);

my $path = shift @ARGV or die "Usage: $0 <svg>\n";

open my $input, '<', $path or die "Unable to read $path: $!\n";
local $/;
my $svg = <$input>;
close $input;

# D2 grid diagrams use direct connections. These routes reproduce the original diagram's
# orthogonal cross-module connections while keeping short relationships unchanged.
my %routes = (
  'location_cell.location.(support_row.testing -&gt; api_row.public)[0]' =>
    'M 1102.500000 321.000000 L 1102.500000 270.000000 L 628.000000 270.000000',
  '(apps.delivery_app -&gt; platforms.delivery_platform.public)[0]' =>
    'M 123.000000 558.000000 L 123.000000 513.000000 M 123.000000 190.000000 L 123.000000 109.000000',
  '(location_cell.location.implementation_row.impl_delivery -&gt; platforms.delivery_platform.public)[0]' =>
    'M 235.500000 416.000000 L 235.500000 109.000000',
  '(apps.navigation_app -&gt; platforms.navigation_platform.public)[0]' =>
    'M 776.000000 558.000000 L 776.000000 513.000000 M 776.000000 190.000000 L 776.000000 109.000000',
  '(apps.navigation_app -&gt; location_cell.location.implementation_row.impl_navigation)[0]' =>
    'M 656.500000 558.000000 L 656.500000 486.000000',
  '(apps.navigation_app -&gt; navigation_cell.navigation.impl)[0]' =>
    'M 951.000000 608.500000 L 1565.500000 608.500000 L 1565.500000 379.000000',
  '(apps.navigation_app -&gt; location_cell.location.implementation_row.robots)[0]' =>
    'M 850.000000 558.000000 L 850.000000 535.000000 L 1104.500000 535.000000 L 1104.500000 486.000000',
  '(location_cell.location.implementation_row.impl_navigation -&gt; platforms.navigation_platform.public)[0]' =>
    'M 656.500000 416.000000 L 656.500000 109.000000',
  '(location_cell.location.implementation_row.impl_navigation -&gt; navigation_cell.navigation.public)[0]' =>
    'M 853.000000 450.000000 L 880.000000 450.000000 L 880.000000 260.000000 L 1369.000000 260.000000',
  '(navigation_cell.navigation.public -&gt; location_cell.location.api_row.public)[0]' =>
    'M 1371.000000 244.000000 L 628.000000 244.000000',
  '(navigation_cell.navigation.public -&gt; location_cell.location.support_row.testing)[0]' =>
    'M 1371.000000 260.000000 L 1320.000000 260.000000 L 1320.000000 355.000000 L 1299.000000 355.000000',
);

my %seen;
my %custom_arrowheads = map { $_ => 1 } (
  '(apps.delivery_app -&gt; platforms.delivery_platform.public)[0]',
  '(location_cell.location.implementation_row.impl_delivery -&gt; platforms.delivery_platform.public)[0]',
  '(apps.navigation_app -&gt; platforms.navigation_platform.public)[0]',
  '(location_cell.location.implementation_row.impl_navigation -&gt; platforms.navigation_platform.public)[0]',
);
$svg =~ s{
  (<g\ class="([^"]+)">(?:(?!<g\ class=).)*?<path\ d=")
  [^"]+
  ("(?:(?!</g>).)*?class="connection"(?:(?!</g>).)*?</g>)
}{
  my ($prefix, $encoded_key, $suffix) = ($1, $2, $3);
  my $key = decode_base64($encoded_key);
  if (exists $routes{$key}) {
    $seen{$key} = 1;
    $suffix =~ s/ marker-end="[^"]+"// if $custom_arrowheads{$key};
    $prefix . $routes{$key} . $suffix;
  } else {
    $&;
  }
}gsex;

for my $key (sort keys %routes) {
  die "Missing routed connection in $path: $key\n" unless $seen{$key};
}

# Paint every connection before the node groups so filled boxes mask any routed segment
# that passes behind them, matching the layering of the original diagram.
my @connections = $svg =~ /(<g class="[^"]+">(?:(?!<g class=).)*?<path\b(?:(?!<\/g>).)*?class="connection"(?:(?!<\/g>).)*?<\/g>)/sg;
for my $connection (@connections) {
  $svg =~ s/\Q$connection\E//;
}
my $connections = join '', @connections;
$svg =~ s{(</style>)}{$1$connections};

# These long platform paths are painted behind the module boxes. Add their tips after the
# boxes so they remain visible while the path segments themselves stay out of the labels.
my $platform_arrowheads = <<'SVG';
<g class="platform-arrowheads"><path d="M 123 133 L 123 115" stroke="#222222" fill="none" class="connection" style="stroke-width:2" /><polygon points="123,105 117,115 129,115" fill="#222222" class="connection" /><path d="M 235.5 133 L 235.5 115" stroke="#222222" fill="none" class="connection" style="stroke-width:2" /><polygon points="235.5,105 229.5,115 241.5,115" fill="#222222" class="connection" /><path d="M 656.5 133 L 656.5 115" stroke="#222222" fill="none" class="connection" style="stroke-width:2" /><polygon points="656.5,105 650.5,115 662.5,115" fill="#222222" class="connection" /><path d="M 776 133 L 776 115" stroke="#222222" fill="none" class="connection" style="stroke-width:2" /><polygon points="776,105 770,115 782,115" fill="#222222" class="connection" /></g>
SVG
$svg =~ s{(<mask id=)}{$platform_arrowheads$1}
  or die "Unable to place platform arrowheads in $path\n";

open my $output, '>', $path or die "Unable to write $path: $!\n";
print {$output} $svg;
close $output;
