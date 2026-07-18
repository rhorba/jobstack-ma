import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AdminMetricsComponent } from './admin-metrics.component';
import { AdminMetrics } from './admin-metrics.model';

describe('AdminMetricsComponent', () => {
  let fixture: ComponentFixture<AdminMetricsComponent>;
  let component: AdminMetricsComponent;
  let httpMock: HttpTestingController;

  const metrics: AdminMetrics = {
    totalPostings: 5,
    liveJobPostings: 3,
    totalApplications: 12,
    confirmedPayments: 3,
    confirmedRevenueMad: 1470,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AdminMetricsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(AdminMetricsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads and displays metrics', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/admin/metrics').flush(metrics);

    expect(component.metrics()).toEqual(metrics);
    expect(component.loading()).toBe(false);
  });

  it('surfaces an error when metrics fail to load', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/admin/metrics').flush('forbidden', { status: 403, statusText: 'Forbidden' });

    expect(component.error()).toBe('Could not load platform metrics.');
    expect(component.loading()).toBe(false);
  });
});
