#!/usr/bin/env perl
# Switches OBJ block models from Porting Lib's loader ("loader": "forge:obj", "porting_lib:loader") to Railways'
# own Fabric model deserializer ("fabric:type": "railways:obj", see ObjModelLoader).
# Usage (from the repo root): perl porting-tools/convert_obj_models.pl $(grep -rl 'forge:obj' src/*/resources/assets --include=*.json)
use strict;
use warnings;

my $count = 0;
for my $file (@ARGV) {
    local $/;
    open my $in, '<:raw', $file or die "$file: $!";
    my $json = <$in>;
    close $in;

    my $orig = $json;
    $json =~ s/"porting_lib:loader"\s*:\s*"porting_lib:obj"\s*,\s*//g;
    $json =~ s/,(\s*)"porting_lib:loader"\s*:\s*"porting_lib:obj"(\s*)}/$1$2}/g;
    $json =~ s/"loader"\s*:\s*"forge:obj"/"fabric:type": "railways:obj"/g;
    next if $json eq $orig;

    open my $out, '>:raw', $file or die "$file: $!";
    print $out $json;
    close $out;
    $count++;
}
print "converted $count models\n";
