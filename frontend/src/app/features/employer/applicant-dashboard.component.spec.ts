import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { ApplicantDashboardComponent } from './applicant-dashboard.component';
import { Applicant } from './applicant.model';

describe('ApplicantDashboardComponent', () => {
  let fixture: ComponentFixture<ApplicantDashboardComponent>;
  let component: ApplicantDashboardComponent;
  let httpMock: HttpTestingController;

  const jobId = '11111111-1111-1111-1111-111111111111';
  const applicants: Applicant[] = [
    {
      applicationId: 'a1',
      fullName: 'Amine Test',
      email: 'amine@test.ma',
      phone: '0600000000',
      cvDownloadUrl: `/api/v1/employers/me/jobs/${jobId}/applicants/p1/cv`,
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApplicantDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: jobId }) } },
        },
      ],
    });
    fixture = TestBed.createComponent(ApplicantDashboardComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads and displays the applicant list', () => {
    fixture.detectChanges();
    httpMock
      .expectOne((r) => r.url === `/api/v1/employers/me/jobs/${jobId}/applicants`)
      .flush({ content: applicants, page: 0, size: 20, totalElements: 1, totalPages: 1 });

    expect(component.applicants()).toEqual(applicants);
    expect(component.loading()).toBe(false);
  });

  it('shows an empty state when there are no applicants', () => {
    fixture.detectChanges();
    httpMock
      .expectOne((r) => r.url === `/api/v1/employers/me/jobs/${jobId}/applicants`)
      .flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

    expect(component.applicants()).toEqual([]);
  });

  it('surfaces an error when the list fails to load (e.g. IDOR block)', () => {
    fixture.detectChanges();
    httpMock
      .expectOne((r) => r.url === `/api/v1/employers/me/jobs/${jobId}/applicants`)
      .flush('forbidden', { status: 403, statusText: 'Forbidden' });

    expect(component.error()).toBe('Could not load applicants for this job posting.');
    expect(component.loading()).toBe(false);
  });
});
