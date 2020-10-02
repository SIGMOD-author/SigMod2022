%% Perch vs HAC
f = figure;
load("microPerchVsHAC.mat");
plot(smooth(perch/1000000, 'rlowess'));
hold on;
hac = hac(:, 1:7);
plot(smooth(hac(:,1)'/1000000), '--');
plot(smooth(hac(:,2)'/1000000), '-.');
plot(smooth(hac(:,3)'/1000000), ':');
legend('Video-zilla', 'HAC-Single', 'HAC-Complete', 'HAC-UPGMA');
xlabel("Number of SVSs");
ylabel("Clustering time (ms)");
save_plot_as(f, 'micro-perch');